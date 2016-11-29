package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.ArrayList;
import java.util.Arrays;
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
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
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
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;




@CommandClass( 
		commandWords={"derive","segments"}, 
		docoptUsages={"<sourceAlmtName> [-r] [-l <linkingReference>] [-s] [-e] (-w <whereClause> | -a) [-m <mergeStrategy>]"},
		docoptOptions={
				"-r, --recursive                                               Include descendent alignments",
				"-l <linkingReference>, --linkingReference <linkingReference>  Reference linking source and target",
				"-s, --suppressSkippedWarning                                  Skip targets without warning",
				"-w <whereClause>, --whereClause <whereClause>                 Qualify source members",
			    "-a, --allMembers                                              Select all source members",
			    "-e, --existingMembersOnly                                     Derive only for existing",
			    "-m <mergeStrategy>, --mergeStrategy <mergeStrategy>           Segment merge strategy"},
		metaTags={CmdMeta.updatesDatabase},
		description="Derive alignment segments from an unconstrained alignment", 
		furtherHelp=
		"The source alignment named by <sourceAlmtName> must exist and be unconstrained. "+
		"Segments will be added to members of one or more target alignments. By default the only target alignment is "+
		"the current alignment. "+
		"If the --recursive option is used, the current alignment's descendents are also included as target alignments. \n"+
		"In order for a target alignment to be updated by this command, its reference must be a member of the source alignment, "+
		"otherwise the target alignment will be skipped, with a warning. The warning can be suppressed using "+
		"the --suppressSkippedWarning option (constrained target only).\n"+
		"The <whereClause> selects members from the source alignment. These members will be "+
		"added to the current alignment if they do not exist, unless --existingMembersOnly is specified. "+
		"The command is available for constrained and unconstrained current alignments. "+
		"However, if the current alignment is unconstrained, an <linkingReference> must be specified, "+
		"This must be a member of both source and target, and must not be one of the selected source members. "+
		"New aligned segments will be added to the current alignment's members, derived "+
		"from the homology in the source alignment between the selected member and the constraining or linking reference member "+
		"The <mergeStrategy> option governs how new segments derived from the source alignment "+
		"are merged with any segments in any existing member of the current alignment.\n"+
		"Possible values for <mergeStrategy> are:\n"+
		"OVERWRITE\n"+"All existing segments are deleted, before new segments are added\n"+
		"MERGE_PREFER_EXISTING\n"+"New segments are merged with existing segments. At reference "+ 
		"locations where they overlap, mappings from existing segments are preferred.\n"+
		"MERGE_PREFER_NEW\n"+"New segments are merged with existing segments. At reference locations where "+
		"they overlap, mappings from new segments are preferred.\n"+
		"The default is MERGE_PREFER_EXISTING.\n"+
		"Example:\n"+
		"  derive segments AL_MASTER -w \"sequence.sequenceID = '3452467'\" -m OVERWRITE") 
public class AlignmentDeriveSegmentsCommand extends AlignmentModeCommand<AlignmentDeriveSegmentsCommand.AlignmentDeriveSegmentsResult> {

	public static final String SOURCE_ALMT_NAME = "sourceAlmtName";
	public static final String RECURSIVE = "recursive";
	public static final String LINKING_REFERENCE = "linkingReference";
	public static final String SUPPRESS_SKIPPED_WARNING = "suppressSkippedWarning";
	public static final String MERGE_STRATEGY = "mergeStrategy";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_MEMBERS = "allMembers";
	public static final String EXISTING_MEMBERS_ONLY = "existingMembersOnly";

	
	private String sourceAlmtName;
	private String linkingReference;
	private Boolean recursive;
	private Boolean suppressSkippedWarning;
	private Optional<Expression> whereClause;
	private Boolean allMembers;
	private Boolean existingMembersOnly;
	private SegmentMergeStrategy segmentMergeStrategy;

	public enum SegmentMergeStrategy {
		OVERWRITE,
		MERGE_PREFER_EXISTING,
		MERGE_PREFER_NEW,
	}


	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		sourceAlmtName = PluginUtils.configureStringProperty(configElem, SOURCE_ALMT_NAME, true);
		recursive = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, RECURSIVE, false)).orElse(false);
		linkingReference = PluginUtils.configureStringProperty(configElem, LINKING_REFERENCE, false);
		suppressSkippedWarning = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, SUPPRESS_SKIPPED_WARNING, false)).orElse(false);
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
		existingMembersOnly = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXISTING_MEMBERS_ONLY, false)).orElse(false);
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allMembers must be specified but not both");
	}

	@Override
	public AlignmentDeriveSegmentsResult execute(CommandContext cmdContext) {
		Alignment currentAlignment = lookupAlignment(cmdContext);
		if(currentAlignment.isConstrained()) {
			if(this.linkingReference != null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "The <linkingReference> option may only be used if the current alignment is unconstrained.");
			}
		} else {
			if(this.recursive) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "The --recursive option may only be used if the current alignment is constrained.");
			}
		}
		Alignment sourceAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(sourceAlmtName));
		if(sourceAlignment.isConstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Source alignment must be unconstrained.");
		}
		String sourceAlignmentName = sourceAlignment.getName();
		SelectQuery selectQuery;
		Expression sourceAlmtMemberExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, sourceAlignmentName);
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(AlignmentMember.class, sourceAlmtMemberExp.andExp(whereClause.get()));
		} else {
			selectQuery = new SelectQuery(AlignmentMember.class, sourceAlmtMemberExp);
		}
		List<AlignmentMember> selectedAlmtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
		
		
		ArrayList<Alignment> targetAlignments = new ArrayList<Alignment>();
		targetAlignments.add(currentAlignment);
		if(recursive) {
			targetAlignments.addAll(currentAlignment.getDescendents());
		}
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();

		for(Alignment targetAlignment: targetAlignments) {
			ReferenceSequence refSequence;
			if(targetAlignment.isConstrained()) {
				refSequence = targetAlignment.getRefSequence();
			} else {
				refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(this.linkingReference));
			}
			Sequence refSeqSeq = refSequence.getSequence();
			String refSequenceSourceName = refSeqSeq.getSource().getName();
			String refSequenceSeqID = refSeqSeq.getSequenceID();
			Map<String, String> refMemberPkMap = AlignmentMember.pkMap(sourceAlignmentName, refSequenceSourceName, refSequenceSeqID);
			AlignmentMember refSeqMemberInSourceAlmt = GlueDataObject.lookup(cmdContext, AlignmentMember.class, refMemberPkMap, true);

			AlignmentMember refSeqMemberInTargetAlmt = null;

			if(!targetAlignment.isConstrained()) {
				if(refSeqMemberInSourceAlmt == null) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Linking reference must be member of source alignment.");
				}
				List<Map<String,String>> selectedSrcAlmtMemberPkMaps = selectedAlmtMembers.stream().map(memb -> memb.pkMap()).collect(Collectors.toList());
				if(selectedSrcAlmtMemberPkMaps.contains(refMemberPkMap)) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Linking reference must not be one of the selected source members.");
				}
				refSeqMemberInTargetAlmt = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
						AlignmentMember.pkMap(targetAlignment.getName(), refSequenceSourceName, refSequenceSeqID), true);
				if(refSeqMemberInTargetAlmt == null) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Linking reference must be member of target alignment.");
				}
			}
			
			if(targetAlignment.isConstrained() && refSeqMemberInSourceAlmt == null) {
				if(!suppressSkippedWarning) {
					GlueLogger.getGlueLogger().warning("Skipping target alignment "+targetAlignment.getName()+
							": its reference sequence "+refSequence.getName()+
							" (source:"+refSequenceSourceName+", sequenceID:"+refSequenceSeqID+") "+
							"is not a member of source alignment "+sourceAlignmentName);
				}
				continue;
			}

			// by inverting the ref source member aligned segments, 
			// we get segments that align the unconstrained source alignment's coordinates to the reference sequence.
			List<QueryAlignedSegment> srcAlmtToRefQaSegs = 
					refSeqMemberInSourceAlmt.getAlignedSegments().stream()
					.map(refAS -> refAS.asQueryAlignedSegment().invert())
					.collect(Collectors.toList());


			for(AlignmentMember sourceAlmtMember: selectedAlmtMembers) {
				Map<String, Object> resultRow = new LinkedHashMap<String, Object>();
				Sequence memberSeq = sourceAlmtMember.getSequence();
				String memberSourceName = memberSeq.getSource().getName();
				String memberSeqID = memberSeq.getSequenceID();

				AlignmentMember currentAlmtMember;
				currentAlmtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
						AlignmentMember.pkMap(targetAlignment.getName(), memberSourceName, memberSeqID), true);
				if(existingMembersOnly) {
					if(currentAlmtMember == null) {
						continue;
					}
				} else {
					if(currentAlmtMember == null) {
						currentAlmtMember = AlignmentAddMemberCommand.addMember(cmdContext, targetAlignment, memberSeq);
					}
				}

				Double prevRefCoverage = currentAlmtMember.getReferenceNtCoveragePercent(cmdContext);

				List<AlignedSegment> existingSegs = new ArrayList<AlignedSegment>(currentAlmtMember.getAlignedSegments());

				List<QueryAlignedSegment> existingQaSegs = existingSegs.stream()
						.map(AlignedSegment::asQueryAlignedSegment)
						.collect(Collectors.toList());

				List<AlignedSegment> memberToSrcSegs = sourceAlmtMember.getAlignedSegments();
				List<QueryAlignedSegment> memberToSrcAlmtQaSegs = memberToSrcSegs.stream()
						.map(AlignedSegment::asQueryAlignedSegment)
						.collect(Collectors.toList());

				List<QueryAlignedSegment> memberToRefSegs = 
						QueryAlignedSegment.translateSegments(memberToSrcAlmtQaSegs, srcAlmtToRefQaSegs);

				List<QueryAlignedSegment> memberToTargetAlmtSegs = null;
				if(targetAlignment.isConstrained()) {
					memberToTargetAlmtSegs = memberToRefSegs;
				} else {
					List<QueryAlignedSegment> refToTargetAlmtSegs = refSeqMemberInTargetAlmt.getAlignedSegments().stream()
					.map(AlignedSegment::asQueryAlignedSegment)
					.collect(Collectors.toList());
					memberToTargetAlmtSegs = QueryAlignedSegment.translateSegments(memberToRefSegs, refToTargetAlmtSegs);

				}
				
				List<QueryAlignedSegment> qaSegsToAdd = null;

				// sort required so that subtract works correctly.
				existingQaSegs = IReferenceSegment.sortByRefStart(existingQaSegs, ArrayList<QueryAlignedSegment>::new);
				memberToTargetAlmtSegs = IReferenceSegment.sortByRefStart(memberToTargetAlmtSegs, ArrayList<QueryAlignedSegment>::new);
				
				switch(segmentMergeStrategy) {
				case OVERWRITE:
					qaSegsToAdd = memberToTargetAlmtSegs;
					break;
				case MERGE_PREFER_EXISTING:
					qaSegsToAdd = new ArrayList<QueryAlignedSegment>();
					qaSegsToAdd.addAll(existingQaSegs);
					qaSegsToAdd.addAll(ReferenceSegment.subtract(memberToTargetAlmtSegs, existingQaSegs));
					break;
				case MERGE_PREFER_NEW:
					qaSegsToAdd = new ArrayList<QueryAlignedSegment>();
					qaSegsToAdd.addAll(ReferenceSegment.subtract(existingQaSegs, memberToTargetAlmtSegs));
					qaSegsToAdd.addAll(memberToTargetAlmtSegs);
					break;
				}

				for(AlignedSegment existingSegment: existingSegs) {
					Map<String, String> pkMap = existingSegment.pkMap();
					GlueDataObject.delete(cmdContext, AlignedSegment.class, pkMap, false);
				}
				cmdContext.commit();

				for(QueryAlignedSegment qaSegmentToAdd: qaSegsToAdd) {
					Map<String, String> pkMap = AlignedSegment.pkMap(targetAlignment.getName(), memberSourceName, memberSeqID, 
							qaSegmentToAdd.getRefStart(), qaSegmentToAdd.getRefEnd(), 
							qaSegmentToAdd.getQueryStart(), qaSegmentToAdd.getQueryEnd());
					AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, pkMap, false);
					alignedSegment.setAlignmentMember(currentAlmtMember);
				}
				cmdContext.commit();

				Double newRefCoverage = currentAlmtMember.getReferenceNtCoveragePercent(cmdContext);

				resultRow.put(AlignmentDeriveSegmentsResult.TARGET_ALIGNMENT_NAME, targetAlignment.getName());
				resultRow.put(AlignmentMember.SOURCE_NAME_PATH, memberSourceName);
				resultRow.put(AlignmentMember.SEQUENCE_ID_PATH, memberSeqID);
				resultRow.put(AlignmentDeriveSegmentsResult.PREV_REF_COVERAGE_PCT, prevRefCoverage);
				resultRow.put(AlignmentDeriveSegmentsResult.NEW_REF_COVERAGE_PCT, newRefCoverage);
				listOfMaps.add(resultRow);

			}
		}
		cmdContext.commit();
		AlignmentDeriveSegmentsResult result = new AlignmentDeriveSegmentsResult(listOfMaps);
		return result;
	}

	


	public static class AlignmentDeriveSegmentsResult extends TableResult {
		public static final String TARGET_ALIGNMENT_NAME = "targetAlmtName";
		public static final String PREV_REF_COVERAGE_PCT = "prevRefCoveragePct";
		public static final String NEW_REF_COVERAGE_PCT = "newRefCoveragePct";
		
		protected AlignmentDeriveSegmentsResult(List<Map<String, Object>> listOfMaps) {
			super("alignmentDeriveSegmentsResult",  
					Arrays.asList(
							TARGET_ALIGNMENT_NAME,
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
			registerDataObjectNameLookup("linkingReference", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerVariableInstantiator("sourceAlmtName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					return GlueDataObject.query(cmdContext, Alignment.class, new SelectQuery(Alignment.class))
							.stream()
							.filter(almt -> !almt.isConstrained())
							.map(almt -> new CompletionSuggestion(almt.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerEnumLookup(MERGE_STRATEGY, SegmentMergeStrategy.class);
		}
	}

	
}
