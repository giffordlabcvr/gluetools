package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.DeriveAlignmentException;
import uk.ac.gla.cvr.gluetools.core.command.DeriveAlignmentException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;




@CommandClass( 
		commandWords={"derive","alignment"}, 
		docoptUsages={"<sourceAlmtName> <targetAlmtName> [-r] [-e] (-w <whereClause> | -a) [-m <mergeStrategy>]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>        Select source  members",
			    "-a, --allMembers                                     Select all source members",
			    "-r, --allowMissingReference                          Warn instead of failing",
			    "-e, --existingMembersOnly                            Derive only for existing",
			    "-m <mergeStrategy>, --mergeStrategy <mergeStrategy>  Segment merge strategy"},
		metaTags={CmdMeta.updatesDatabase},
		description="Derive constrained alignment segments from an unconstrained alignment", 
		furtherHelp="The source alignment named by <sourceAlmtName> must exist. "+
		"The target alignment named by <targetAlmtName> must be constrained to a reference sequence. "+
		"The reference sequence of the target alignment must be a member of the source alignment. "+
		"If it is not a member, an exception will be thrown, unless <allowMissingReference> is specified, "+
		"in which case a warning is emitted.\n"+
		"The <whereClause> selects members from the source alignment. These members will be "+
		"added to the target alignment if they do not exist, unless <existingMembersOnly> is specified. "+
		"New aligned segments will be added to the target alignment, derived "+
		"from the homology between the member and reference sequence in the source alignment. \n"+
		"The <mergeStrategy> option governs how new segments derived from the source alignment "+
		"are merged with any segments in any existing alignment member of the target alignment.\n"+
		"Possible values for <mergeStrategy> are:\n"+
		"OVERWRITE\n"+"All existing segments are deleted, before new segments are added\n"+
		"MERGE_PREFER_EXISTING\n"+"New segments are merged with existing segments. At reference "+ 
		"locations where they overlap, mappings from existing segments are preferred.\n"+
		"MERGE_PREFER_NEW\n"+"New segments are merged with existing segments. At reference locations where "+
		"they overlap, mappings from new segments are preferred.\n"+
		"The default is OVERWRITE.\n"+
		"Example:\n"+
		"  derive alignment AL_MASTER AL_GENOTYPE_3 -w \"sequence.sequenceID = '3452467'\" -m OVERWRITE") 
public class DeriveAlignmentCommand extends ProjectModeCommand<DeriveAlignmentCommand.DeriveAlignmentResult> {

	public static final String SOURCE_ALMT_NAME = "sourceAlmtName";
	public static final String TARGET_ALMT_NAME = "targetAlmtName";
	public static final String MERGE_STRATEGY = "mergeStrategy";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String EXISTING_MEMBERS_ONLY = "existingMembersOnly";
	public static final String ALLOW_MISSING_REFERENCE = "allowMissingReference";

	
	private String sourceAlmtName;
	private String targetAlmtName;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private Boolean allowMissingReference;
	private Boolean existingMembersOnly;
	private SegmentMergeStrategy segmentMergeStrategy;

	public enum SegmentMergeStrategy {
		OVERWRITE,
		MERGE_PREFER_EXISTING,
		MERGE_PREFER_NEW,
	}


	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		sourceAlmtName = PluginUtils.configureStringProperty(configElem, SOURCE_ALMT_NAME, true);
		targetAlmtName = PluginUtils.configureStringProperty(configElem, TARGET_ALMT_NAME, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		allMembers = PluginUtils.configureBooleanProperty(configElem, ALL_MEMBERS, true);
		if(!(
				(!whereClause.isPresent() && allMembers)||
				(whereClause.isPresent() && !allMembers)
			)) {
			usageError();
		}
		segmentMergeStrategy = Optional
				.ofNullable(PluginUtils.configureEnumProperty(SegmentMergeStrategy.class, configElem, MERGE_STRATEGY, false))
				.orElse(SegmentMergeStrategy.MERGE_PREFER_EXISTING);
		allowMissingReference = PluginUtils.configureBooleanProperty(configElem, ALLOW_MISSING_REFERENCE, true);
		existingMembersOnly = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXISTING_MEMBERS_ONLY, false)).orElse(false);

	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allMembers must be specified but not both");
	}

	@Override
	public DeriveAlignmentResult execute(CommandContext cmdContext) {
		
		Alignment sourceAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(sourceAlmtName));
		Alignment targetAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(targetAlmtName));
		ReferenceSequence refSequence = targetAlignment.getRefSequence();
		if(refSequence == null) {
			throw new DeriveAlignmentException(Code.TARGET_ALIGNMENT_IS_UNCONSTRAINED, targetAlignment.getName());
		}
		Sequence refSeqSeq = refSequence.getSequence();
		String refSequenceSourceName = refSeqSeq.getSource().getName();
		String refSequenceSeqID = refSeqSeq.getSequenceID();
		AlignmentMember refSeqMemberInSourceAlmt = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(sourceAlignment.getName(), refSequenceSourceName, refSequenceSeqID), true);
		if(refSeqMemberInSourceAlmt == null) {
			if(allowMissingReference) {
				GlueLogger.getGlueLogger().warning("Reference sequence "+refSequence.getName()+
						" (source:"+refSequenceSourceName+", seqID:"+refSequenceSeqID+") of target alignment "+
						targetAlignment.getName()+" is not a member of source alignment "+sourceAlignment.getName()); 
				return new DeriveAlignmentResult(Collections.emptyList());
			}
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
		List<AlignmentMember> sourceAlmtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();
		
		for(AlignmentMember sourceAlmtMember: sourceAlmtMembers) {
			Map<String, Object> resultRow = new LinkedHashMap<String, Object>();
			Sequence memberSeq = sourceAlmtMember.getSequence();
			String memberSourceName = memberSeq.getSource().getName();
			String memberSeqID = memberSeq.getSequenceID();
			
			AlignmentMember targetAlmtMember;
			if(existingMembersOnly) {
				targetAlmtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
						AlignmentMember.pkMap(targetAlignment.getName(), memberSourceName, memberSeqID), true);
				if(targetAlmtMember == null) {
					continue;
				}
			} else {
				targetAlmtMember = GlueDataObject.create(cmdContext, AlignmentMember.class, 
						AlignmentMember.pkMap(targetAlignment.getName(), memberSourceName, memberSeqID), true);
				targetAlmtMember.setAlignment(targetAlignment);
				targetAlmtMember.setSequence(memberSeq);
			}
			
			double prevRefCoverage = targetAlmtMember.getReferenceNtCoveragePercent(cmdContext);
			
			List<AlignedSegment> existingSegs = new ArrayList<AlignedSegment>(targetAlmtMember.getAlignedSegments());

			List<QueryAlignedSegment> existingQaSegs = existingSegs.stream()
					.map(AlignedSegment::asQueryAlignedSegment)
					.collect(Collectors.toList());

			List<AlignedSegment> memberToSrcSegs = sourceAlmtMember.getAlignedSegments();
			List<QueryAlignedSegment> memberToSrcAlmtQaSegs = memberToSrcSegs.stream()
					.map(AlignedSegment::asQueryAlignedSegment)
					.collect(Collectors.toList());
			
			List<QueryAlignedSegment> newQaSegs = 
					QueryAlignedSegment.translateSegments(memberToSrcAlmtQaSegs, srcAlmtToRefQaSegs);

			List<QueryAlignedSegment> qaSegsToAdd = null;

			switch(segmentMergeStrategy) {
			case OVERWRITE:
				qaSegsToAdd = newQaSegs;
				break;
			case MERGE_PREFER_EXISTING:
				qaSegsToAdd = new ArrayList<QueryAlignedSegment>();
				qaSegsToAdd.addAll(existingQaSegs);
				qaSegsToAdd.addAll(ReferenceSegment.subtract(newQaSegs, existingQaSegs));
				break;
			case MERGE_PREFER_NEW:
				qaSegsToAdd = new ArrayList<QueryAlignedSegment>();
				qaSegsToAdd.addAll(ReferenceSegment.subtract(existingQaSegs, newQaSegs));
				qaSegsToAdd.addAll(newQaSegs);
				break;
			}
			
			for(AlignedSegment existingSegment: existingSegs) {
				GlueDataObject.delete(cmdContext, AlignedSegment.class, existingSegment.pkMap(), false);
			}
			cmdContext.commit();

			for(QueryAlignedSegment qaSegmentToAdd: qaSegsToAdd) {
				AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, 
						AlignedSegment.pkMap(targetAlignment.getName(), memberSourceName, memberSeqID, 
								qaSegmentToAdd.getRefStart(), qaSegmentToAdd.getRefEnd(), 
								qaSegmentToAdd.getQueryStart(), qaSegmentToAdd.getQueryEnd())
						, false);
				alignedSegment.setAlignmentMember(targetAlmtMember);
			}
			cmdContext.commit();

			double newRefCoverage = targetAlmtMember.getReferenceNtCoveragePercent(cmdContext);

			resultRow.put(AlignmentMember.SOURCE_NAME_PATH, memberSourceName);
			resultRow.put(AlignmentMember.SEQUENCE_ID_PATH, memberSeqID);
			resultRow.put(DeriveAlignmentResult.PREV_REF_COVERAGE_PCT, prevRefCoverage);
			resultRow.put(DeriveAlignmentResult.NEW_REF_COVERAGE_PCT, newRefCoverage);
			listOfMaps.add(resultRow);
			
		}
		cmdContext.commit();
		DeriveAlignmentResult result = new DeriveAlignmentResult(listOfMaps);
		return result;
	}

	


	public static class DeriveAlignmentResult extends TableResult {
		public static final String PREV_REF_COVERAGE_PCT = "prevRefCoveragePct";
		public static final String NEW_REF_COVERAGE_PCT = "newRefCoveragePct";
		
		protected DeriveAlignmentResult(List<Map<String, Object>> listOfMaps) {
			super("deriveAlignmentResult",  
					Arrays.asList(
							AlignmentMember.SOURCE_NAME_PATH, 
							AlignmentMember.SEQUENCE_ID_PATH, 
							PREV_REF_COVERAGE_PCT,
							NEW_REF_COVERAGE_PCT),
					listOfMaps);
		}
	}
	
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup(SOURCE_ALMT_NAME, Alignment.class, Alignment.NAME_PROPERTY);
			registerDataObjectNameLookup(TARGET_ALMT_NAME, Alignment.class, Alignment.NAME_PROPERTY);
			registerEnumLookup(MERGE_STRATEGY, SegmentMergeStrategy.class);
		}
	}

	
}
