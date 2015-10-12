package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.DeriveAlignmentException;
import uk.ac.gla.cvr.gluetools.core.command.DeriveAlignmentException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@CommandClass( 
		commandWords={"derive","alignment"}, 
		docoptUsages={"<sourceAlmtName> <targetAlmtName> (-w <whereClause> | -a)"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Select source  members",
			    "-a, --allMembers                               Select all source members"},
		metaTags={CmdMeta.updatesDatabase},
		description="Derive constrained alignment segments from an unconstrained alignment", 
		furtherHelp="The source alignment named by <sourceAlmtName> must exist. "+
		"The target alignment named by <targetAlmtName> must be constrained to a reference sequence. "+
		"The reference sequence of the target alignment must be a member of the source alignment. "+
		"The <whereClause> selects members from the source alignment. These members will be "+
		"added to the target alignment if they do not exist. Any aligned segments these members have in the "+
		"target alignment will be deleted. New aligned segments will be added to the target alignment, derived "+
		"from the homology between the member and reference sequence in the source alignment. \n"+
		"Example:\n"+
		"  derive alignment AL_MASTER AL_GENOTYPE_3 -w \"sequence.sequenceID = '3452467'\"") 
public class DeriveAlignmentCommand extends ProjectModeCommand<DeriveAlignmentCommand.DeriveAlignmentResult> {

	private String sourceAlmtName;
	private String targetAlmtName;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		sourceAlmtName = PluginUtils.configureStringProperty(configElem, "sourceAlmtName", true);
		targetAlmtName = PluginUtils.configureStringProperty(configElem, "targetAlmtName", true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, "whereClause", false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, "allMembers", true);
		if(!(
				(!whereClause.isPresent() && allMembers)||
				(whereClause.isPresent() && !allMembers)
			)) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allMembers must be specified but not both");
	}

	@Override
	public DeriveAlignmentResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Alignment sourceAlignment = GlueDataObject.lookup(objContext, Alignment.class, Alignment.pkMap(sourceAlmtName));
		Alignment targetAlignment = GlueDataObject.lookup(objContext, Alignment.class, Alignment.pkMap(targetAlmtName));
		ReferenceSequence refSequence = targetAlignment.getRefSequence();
		if(refSequence == null) {
			throw new DeriveAlignmentException(Code.TARGET_ALIGNMENT_IS_UNCONSTRAINED, targetAlignment.getName());
		}
		Sequence refSeqSeq = refSequence.getSequence();
		String refSequenceSourceName = refSeqSeq.getSource().getName();
		String refSequenceSeqID = refSeqSeq.getSequenceID();
		AlignmentMember refSeqMemberInSourceAlmt = GlueDataObject.lookup(objContext, AlignmentMember.class, 
				AlignmentMember.pkMap(sourceAlignment.getName(), refSequenceSourceName, refSequenceSeqID), true);
		if(refSeqMemberInSourceAlmt == null) {
			throw new DeriveAlignmentException(Code.REFERENCE_SEQUENCE_NOT_MEMBER_OF_SOURCE_ALIGNMENT, 
					sourceAlignment.getName(), refSequence.getName(), refSequenceSourceName, refSequenceSeqID,
					targetAlignment.getName());
		}
		
		// by inverting the ref seq member aligned segments, 
		// we get segments that align the source alignment's coordinates to the reference sequence.
		List<QueryAlignedSegment> srcAlmtToRefQaSegs = 
				refSeqMemberInSourceAlmt.getAlignedSegments().stream()
				.map(refAS -> refAS.asQueryAlignedSegment().invert())
				.collect(Collectors.toList());
		
		SelectQuery selectQuery;
		Expression sourceAlmtMemberExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, sourceAlignment.getName());
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(AlignmentMember.class, sourceAlmtMemberExp.andExp(whereClause.get()));
		} else {
			selectQuery = new SelectQuery(AlignmentMember.class, sourceAlmtMemberExp);
		}
		List<AlignmentMember> sourceAlmtMembers = GlueDataObject.query(objContext, AlignmentMember.class, selectQuery);
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();
		
		for(AlignmentMember sourceAlmtMember: sourceAlmtMembers) {
			Map<String, Object> resultRow = new LinkedHashMap<String, Object>();
			Sequence memberSeq = sourceAlmtMember.getSequence();
			String memberSourceName = memberSeq.getSource().getName();
			String memberSeqID = memberSeq.getSequenceID();
			
			AlignmentMember targetAlmtMember = GlueDataObject.create(objContext, AlignmentMember.class, 
					AlignmentMember.pkMap(targetAlignment.getName(), memberSourceName, memberSeqID), true);
			targetAlmtMember.setAlignment(targetAlignment);
			targetAlmtMember.setSequence(memberSeq);
			
			List<AlignedSegment> segmentsToRemove = new ArrayList<AlignedSegment>(targetAlmtMember.getAlignedSegments());
			for(AlignedSegment segmentToRemove: segmentsToRemove) {
				GlueDataObject.delete(objContext, AlignedSegment.class, segmentToRemove.pkMap(), false);
			}
			
			List<QueryAlignedSegment> memberToSrcAlmtQaSegs = sourceAlmtMember.getAlignedSegments().stream()
					.map(AlignedSegment::asQueryAlignedSegment)
					.collect(Collectors.toList());
			
			List<QueryAlignedSegment> memberToRefQaSegs = 
					QueryAlignedSegment.translateSegments(memberToSrcAlmtQaSegs, srcAlmtToRefQaSegs);

			for(QueryAlignedSegment qaSegmentToAdd: memberToRefQaSegs) {
				AlignedSegment alignedSegment = GlueDataObject.create(objContext, AlignedSegment.class, 
						AlignedSegment.pkMap(targetAlignment.getName(), memberSourceName, memberSeqID, 
								qaSegmentToAdd.getRefStart(), qaSegmentToAdd.getRefEnd(), 
								qaSegmentToAdd.getQueryStart(), qaSegmentToAdd.getQueryEnd())
						, false);
				alignedSegment.setAlignmentMember(targetAlmtMember);
			}
			
			resultRow.put(AlignmentMember.SOURCE_NAME_PATH, memberSourceName);
			resultRow.put(AlignmentMember.SEQUENCE_ID_PATH, memberSeqID);
			resultRow.put(DeriveAlignmentResult.REMOVED_SEGMENTS, segmentsToRemove.size());
			resultRow.put(DeriveAlignmentResult.ADDED_SEGMENTS, memberToRefQaSegs.size());
			listOfMaps.add(resultRow);
			
		}
		cmdContext.commit();
		DeriveAlignmentResult result = new DeriveAlignmentResult(listOfMaps);
		return result;
	}

	


	public static class DeriveAlignmentResult extends TableResult {
		public static final String REMOVED_SEGMENTS = "removedSegments";
		public static final String ADDED_SEGMENTS = "addedSegments";
		
		protected DeriveAlignmentResult(List<Map<String, Object>> listOfMaps) {
			super("deriveAlignmentResult",  
					Arrays.asList(
							AlignmentMember.SOURCE_NAME_PATH, 
							AlignmentMember.SEQUENCE_ID_PATH, 
							REMOVED_SEGMENTS,
							ADDED_SEGMENTS),
					listOfMaps);
		}
	}
	
}
