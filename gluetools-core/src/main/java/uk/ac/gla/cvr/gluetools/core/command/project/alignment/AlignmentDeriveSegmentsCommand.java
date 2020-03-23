/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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

/**
 * Current alignment	Source alignment	Notes
 *
 * constrained			constrained			No linkingReference specified. Alignments must have same constraining reference.
 * constrained			unconstrained		No linkingReference specified. Constraining ref must be member of source alignment, otherwise skipped as target.
 * unconstrained		constrained			No linkingReference specified, linking reference is implicitly source alignment's constraining reference.
 * unconstrained		unconstrained		Must specify linkingReference, a member of both alignments not selected by whereClause.
 *
 */


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
		metaTags = {},
		description="Derive alignment segments from another alignment", 
		furtherHelp=
		"Segments will be added to members of one or more target alignments. By default the only target alignment is "+
		"the current alignment. "+
		"If the --recursive option is used, and the current alignment is constrained, the current alignment's descendents "+
		"are also included as target alignments. \n"+
		"In order for a constrained target alignment to be updated by this command, its reference must be a member of the unconstrained source alignment, "+
		"or if the source alignment is constrained, they must have the same reference. "+
		"If these conditions are not met, the target alignment will be skipped, with a warning. The warning can be suppressed using "+
		"the --suppressSkippedWarning option.\n"+
		"The <whereClause> selects members from the source alignment. These members will be "+
		"added to the current alignment if they do not exist, unless --existingMembersOnly is specified. "+
		"The --existingMembersOnly option must be used if --recursive is used. "+
		"The command is available for constrained and unconstrained current alignments. "+
		"However, if both the source and current alignment are unconstrained, a <linkingReference> must be specified, "+
		"This must be a member of both source and target, and must not be one of the selected source members. "+
		"If the source is constrained and current unconstrained, the linking reference is implicitly the source alignment's constraining reference. "+
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
		if(recursive && !existingMembersOnly) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "The --existingMembersOnly option must be used if --recursive is used.");
		}
		
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allMembers must be specified but not both");
	}

	@Override
	public AlignmentDeriveSegmentsResult execute(CommandContext cmdContext) {
		Alignment currentAlignment = lookupAlignment(cmdContext);
		boolean currentAlignmentConstrained = currentAlignment.isConstrained();
		Alignment sourceAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(sourceAlmtName));
		boolean sourceAlignmentConstrained = sourceAlignment.isConstrained();

		if(currentAlignment.getName().equals(sourceAlignment.getName())) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Source and current alignment cannot be the same.");
		}

		if(this.recursive && !currentAlignmentConstrained) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "The --recursive option may only be used if the current alignment is constrained.");
		}

		if(this.linkingReference != null && (currentAlignmentConstrained || sourceAlignmentConstrained)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "The <linkingReference> option may be used only when both source and current alignment are unconstrained.");
		}

		if(this.linkingReference == null && (!currentAlignmentConstrained) && (!sourceAlignmentConstrained)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "The <linkingReference> option must be used when both source and current alignment are unconstrained.");
		}

		String sourceAlignmentName = sourceAlignment.getName();
		ArrayList<Alignment> targetAlignments = new ArrayList<Alignment>();
		targetAlignments.add(currentAlignment);
		if(recursive) {
			List<Alignment> descendents = currentAlignment.getDescendents();
			for(Alignment decAlmt: descendents) {
				if(decAlmt.getName().equals(sourceAlignment.getName())) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "A descendent of the current alignment is the source alignment.");
				}
			}
			targetAlignments.addAll(descendents);
		}
		List<String> targetAlignmentNames = targetAlignments.stream().map(al -> al.getName()).collect(Collectors.toList());
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();

		for(String targetAlignmentName: targetAlignmentNames) {
			processTargetAlignment(cmdContext, sourceAlignmentName, listOfMaps, targetAlignmentName);
		}
		AlignmentDeriveSegmentsResult result = new AlignmentDeriveSegmentsResult(listOfMaps);
		return result;
	}

	private void processTargetAlignment(CommandContext cmdContext, String sourceAlignmentName, List<Map<String, Object>> listOfMaps, String targetAlignmentName) {
		Alignment sourceAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(sourceAlignmentName));
		GlueLogger.getGlueLogger().finest("Processing target alignment "+targetAlignmentName);
		Alignment targetAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(targetAlignmentName));
		boolean sourceAlignmentConstrained = sourceAlignment.isConstrained();
		boolean targetAlignmentConstrained = targetAlignment.isConstrained();
		
		// linking / constraining reference
		ReferenceSequence refSequence = null;
		
		// membership of this reference in source / target alignments
		Map<String, String> sourceRefMemberPkMap = null;
		AlignmentMember refSeqMemberInSourceAlmt = null;
		Map<String, String> targetRefMemberPkMap = null;
		AlignmentMember refSeqMemberInTargetAlmt = null;

		
		if(targetAlignmentConstrained && sourceAlignmentConstrained) {
			refSequence = targetAlignment.getRefSequence();
			if(!refSequence.getName().equals(sourceAlignment.getRefSequence().getName())) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Where both source and target are constrained, they must have the same reference.");
			}
		} else if(targetAlignmentConstrained && (!sourceAlignmentConstrained)) {
			refSequence = targetAlignment.getRefSequence();
			Sequence refSeqSeq = refSequence.getSequence();
			String refSequenceSourceName = refSeqSeq.getSource().getName();
			String refSequenceSeqID = refSeqSeq.getSequenceID();
			sourceRefMemberPkMap = AlignmentMember.pkMap(sourceAlignmentName, refSequenceSourceName, refSequenceSeqID);
			refSeqMemberInSourceAlmt = GlueDataObject.lookup(cmdContext, AlignmentMember.class, sourceRefMemberPkMap, true);
			if(refSeqMemberInSourceAlmt == null) {
				if(!suppressSkippedWarning) {
					GlueLogger.getGlueLogger().warning("Skipping target alignment "+targetAlignmentName+
							": its reference sequence "+refSequence.getName()+
							" (source:"+refSequenceSourceName+", sequenceID:"+refSequenceSeqID+") "+
							"is not a member of source alignment "+sourceAlignmentName);
				}
				return;
			}
		} else if((!targetAlignmentConstrained) && sourceAlignmentConstrained) {
			refSequence = sourceAlignment.getRefSequence();
			Sequence refSeqSeq = refSequence.getSequence();
			String refSequenceSourceName = refSeqSeq.getSource().getName();
			String refSequenceSeqID = refSeqSeq.getSequenceID();
			targetRefMemberPkMap = AlignmentMember.pkMap(targetAlignmentName, refSequenceSourceName, refSequenceSeqID);
			refSeqMemberInTargetAlmt = GlueDataObject.lookup(cmdContext, AlignmentMember.class, targetRefMemberPkMap, true);
			if(refSeqMemberInTargetAlmt == null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Source constraining reference must be member of target alignment.");
			}
		} else if((!targetAlignmentConstrained) && (!sourceAlignmentConstrained)) {
			refSequence = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(this.linkingReference));
			Sequence refSeqSeq = refSequence.getSequence();
			String refSequenceSourceName = refSeqSeq.getSource().getName();
			String refSequenceSeqID = refSeqSeq.getSequenceID();
			targetRefMemberPkMap = AlignmentMember.pkMap(targetAlignmentName, refSequenceSourceName, refSequenceSeqID);
			refSeqMemberInTargetAlmt = GlueDataObject.lookup(cmdContext, AlignmentMember.class, targetRefMemberPkMap, true);
			if(refSeqMemberInTargetAlmt == null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Linking reference must be member of target alignment.");
			}
			sourceRefMemberPkMap = AlignmentMember.pkMap(sourceAlignmentName, refSequenceSourceName, refSequenceSeqID);
			refSeqMemberInSourceAlmt = GlueDataObject.lookup(cmdContext, AlignmentMember.class, sourceRefMemberPkMap, true);
			if(refSeqMemberInSourceAlmt == null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Linking reference must be member of source alignment.");
			}
		}

		List<QueryAlignedSegment> srcAlmtToRefQaSegs = null;
		
		if(!sourceAlignmentConstrained) {
			// by inverting the ref source member aligned segments, 
			// we get segments that align the unconstrained source alignment's coordinates to the reference sequence.
			srcAlmtToRefQaSegs = QueryAlignedSegment.invertList(refSeqMemberInSourceAlmt.segmentsAsQueryAlignedSegments());
		}

		SelectQuery selectQuery;
		Expression sourceAlmtMemberExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, sourceAlignmentName);
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(AlignmentMember.class, sourceAlmtMemberExp.andExp(whereClause.get()));
		} else {
			selectQuery = new SelectQuery(AlignmentMember.class, sourceAlmtMemberExp);
		}

		int numberToProcess = GlueDataObject.count(cmdContext, selectQuery);
		int batchSize = 250;

		selectQuery.setFetchLimit(batchSize);
		selectQuery.setPageSize(batchSize);
		int offset = 0;
		while(offset < numberToProcess) {
			Alignment targetAlmtForCtx = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(targetAlignmentName));
			selectQuery.setFetchOffset(offset);
			int lastBatchIndex = Math.min(offset+batchSize, numberToProcess);
			GlueLogger.getGlueLogger().finest("Retrieving source alignment members "+(offset+1)+" to "+lastBatchIndex+" of "+numberToProcess);
			List<AlignmentMember> selectedAlmtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, selectQuery);
			GlueLogger.getGlueLogger().finest("Processing source alignment members "+(offset+1)+" to "+lastBatchIndex+" of "+numberToProcess);

			// each batch is processed in 2 commits.

			// step 1, add new members where necessary, delete existing segments, but record delete segments in map.
			Map<Map<String, String>, List<QueryAlignedSegment>> targetMemberPkMapToExistingQaSegs = 
					new LinkedHashMap<Map<String,String>, List<QueryAlignedSegment>>();
			Map<Map<String, String>, AlignmentMember> targetMemberPkMapToAlmtMember = 
					new LinkedHashMap<Map<String,String>, AlignmentMember>();

			updateMembersForBatch(cmdContext, targetAlignmentName,
					sourceRefMemberPkMap, targetAlmtForCtx, selectedAlmtMembers,
					targetMemberPkMapToExistingQaSegs,
					targetMemberPkMapToAlmtMember);
			cmdContext.commit();

			// step 2, compute new segments, update them in the db.
			computeNewSegmentsForBatch(cmdContext, listOfMaps,
					targetAlignmentName, refSeqMemberInTargetAlmt,
					srcAlmtToRefQaSegs, targetAlmtForCtx, selectedAlmtMembers,
					targetMemberPkMapToExistingQaSegs,
					targetMemberPkMapToAlmtMember);
			cmdContext.commit();
			cmdContext.newObjectContext();
			offset = offset+batchSize;
		} 	
	}

	private void computeNewSegmentsForBatch(
			CommandContext cmdContext,
			List<Map<String, Object>> listOfMaps,
			String targetAlignmentName,
			AlignmentMember refSeqMemberInTargetAlmt,
			List<QueryAlignedSegment> srcAlmtToRefQaSegs,
			Alignment targetAlmtForCtx,
			List<AlignmentMember> selectedAlmtMembers,
			Map<Map<String, String>, List<QueryAlignedSegment>> targetMemberPkMapToExistingQaSegs,
			Map<Map<String, String>, AlignmentMember> targetMemberPkMapToAlmtMember) {
		for(AlignmentMember sourceAlmtMember: selectedAlmtMembers) {

			String targetMemberSourceName = sourceAlmtMember.getSequence().getSource().getName();
			String targetMemberSequenceID = sourceAlmtMember.getSequence().getSequenceID();
			Map<String, String> targetMemberPkMap = 
					AlignmentMember.pkMap(targetAlignmentName, targetMemberSourceName, targetMemberSequenceID);
			AlignmentMember targetAlmtMember = targetMemberPkMapToAlmtMember.get(targetMemberPkMap);
			if(targetAlmtMember == null) {
				continue;
			}
			List<QueryAlignedSegment> targetMemberExistingQaSegs = targetMemberPkMapToExistingQaSegs.get(targetMemberPkMap);

			Double prevRefCoverage = targetAlmtMember.getReferenceNtCoveragePercent(cmdContext);

			List<QueryAlignedSegment> memberToSrcAlmtQaSegs = sourceAlmtMember.segmentsAsQueryAlignedSegments();

			List<QueryAlignedSegment> memberToRefSegs;
			if(srcAlmtToRefQaSegs != null) {
				memberToRefSegs = QueryAlignedSegment.translateSegments(memberToSrcAlmtQaSegs, srcAlmtToRefQaSegs);
			} else {
				// source alignment constrained.
				memberToRefSegs = memberToSrcAlmtQaSegs;
			}

			List<QueryAlignedSegment> memberToTargetAlmtSegs = null;
			if(targetAlmtForCtx.isConstrained()) {
				memberToTargetAlmtSegs = memberToRefSegs;
			} else {
				List<QueryAlignedSegment> refToTargetAlmtSegs = refSeqMemberInTargetAlmt.segmentsAsQueryAlignedSegments();
				memberToTargetAlmtSegs = QueryAlignedSegment.translateSegments(memberToRefSegs, refToTargetAlmtSegs);
			}

			List<QueryAlignedSegment> qaSegsToAdd = null;

			// sort required so that subtract works correctly.
			targetMemberExistingQaSegs = IReferenceSegment.sortByRefStart(targetMemberExistingQaSegs, ArrayList<QueryAlignedSegment>::new);
			memberToTargetAlmtSegs = IReferenceSegment.sortByRefStart(memberToTargetAlmtSegs, ArrayList<QueryAlignedSegment>::new);

			switch(segmentMergeStrategy) {
			case OVERWRITE:
				qaSegsToAdd = memberToTargetAlmtSegs;
				break;
			case MERGE_PREFER_EXISTING:
				qaSegsToAdd = new ArrayList<QueryAlignedSegment>();
				qaSegsToAdd.addAll(targetMemberExistingQaSegs);
				qaSegsToAdd.addAll(ReferenceSegment.subtract(memberToTargetAlmtSegs, targetMemberExistingQaSegs));
				break;
			case MERGE_PREFER_NEW:
				qaSegsToAdd = new ArrayList<QueryAlignedSegment>();
				qaSegsToAdd.addAll(ReferenceSegment.subtract(targetMemberExistingQaSegs, memberToTargetAlmtSegs));
				qaSegsToAdd.addAll(memberToTargetAlmtSegs);
				break;
			}
			for(QueryAlignedSegment qaSegmentToAdd: qaSegsToAdd) {
				Map<String, String> pkMap = AlignedSegment.pkMap(targetAlignmentName, targetMemberSourceName, targetMemberSequenceID, 
						qaSegmentToAdd.getRefStart(), qaSegmentToAdd.getRefEnd(), 
						qaSegmentToAdd.getQueryStart(), qaSegmentToAdd.getQueryEnd());
				AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, pkMap, false);
				alignedSegment.setAlignmentMember(targetAlmtMember);
			}
			Double newRefCoverage = targetAlmtMember.getReferenceNtCoveragePercent(cmdContext);

			Map<String, Object> resultRow = new LinkedHashMap<String, Object>();
			resultRow.put(AlignmentDeriveSegmentsResult.TARGET_ALIGNMENT_NAME, targetAlignmentName);
			resultRow.put(AlignmentMember.SOURCE_NAME_PATH, targetMemberSourceName);
			resultRow.put(AlignmentMember.SEQUENCE_ID_PATH, targetMemberSequenceID);
			resultRow.put(AlignmentDeriveSegmentsResult.PREV_REF_COVERAGE_PCT, prevRefCoverage);
			resultRow.put(AlignmentDeriveSegmentsResult.NEW_REF_COVERAGE_PCT, newRefCoverage);
			listOfMaps.add(resultRow);
		}
	}

	private void updateMembersForBatch(
			CommandContext cmdContext,
			String targetAlignmentName,
			Map<String, String> sourceRefMemberPkMap,
			Alignment targetAlmtForCtx,
			List<AlignmentMember> selectedAlmtMembers,
			Map<Map<String, String>, List<QueryAlignedSegment>> targetMemberPkMapToExistingQaSegs,
			Map<Map<String, String>, AlignmentMember> targetMemberPkMapToAlmtMember) {
		// create an expression to retrieve the set of corresponding members of the target alignment
		Expression sourceSeqExp = ExpressionFactory.expFalse();
		for(AlignmentMember sourceAlmtMember: selectedAlmtMembers) {
			sourceSeqExp = sourceSeqExp.orExp(
					ExpressionFactory
					.matchExp(AlignmentMember.SOURCE_NAME_PATH, 
							sourceAlmtMember.getSequence().getSource().getName())
							.andExp(							ExpressionFactory
									.matchExp(AlignmentMember.SEQUENCE_ID_PATH, 
											sourceAlmtMember.getSequence().getSequenceID()))
					);
		}				
		Expression targetMembersExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, targetAlignmentName)
				.andExp(sourceSeqExp);
		SelectQuery targetMembersQuery = new SelectQuery(AlignmentMember.class, targetMembersExp);
		List<AlignmentMember> targetMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, targetMembersQuery);
		for(AlignmentMember targetMember: targetMembers) {
			targetMemberPkMapToAlmtMember.put(targetMember.pkMap(), targetMember);
		}

		for(AlignmentMember sourceAlmtMember: selectedAlmtMembers) {


			Sequence memberSeq = sourceAlmtMember.getSequence();
			String memberSourceName = memberSeq.getSource().getName();
			String memberSeqID = memberSeq.getSequenceID();

			Map<String, String> targetMemberPkMap = AlignmentMember.pkMap(targetAlignmentName, memberSourceName, memberSeqID);

			AlignmentMember targetAlmtMember = targetMemberPkMapToAlmtMember.get(targetMemberPkMap);


			if(existingMembersOnly) {
				if(targetAlmtMember == null) {
					continue;
				}
			} else {
				if(targetAlmtMember == null) {
					targetAlmtMember = AlignmentAddMemberCommand.addMember(cmdContext, targetAlmtForCtx, memberSeq, false);
					targetMemberPkMapToAlmtMember.put(targetMemberPkMap, targetAlmtMember);
				}
			}

			if( targetAlmtMember != null && (!targetAlmtMember.getAlignment().isConstrained()) &&
					sourceRefMemberPkMap != null && sourceAlmtMember.pkMap().equals(sourceRefMemberPkMap)) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Linking / constraining reference must not be one of the selected source members.");
			}

			List<AlignedSegment> targetMemberExistingSegs = new ArrayList<AlignedSegment>(targetAlmtMember.getAlignedSegments());

			List<QueryAlignedSegment> targetMemberExistingQaSegs = targetMemberExistingSegs.stream()
					.map(AlignedSegment::asQueryAlignedSegment)
					.collect(Collectors.toList());

			targetMemberPkMapToExistingQaSegs.put(targetAlmtMember.pkMap(), targetMemberExistingQaSegs);

			for(AlignedSegment existingSegment: targetMemberExistingSegs) {
				Map<String, String> pkMap = existingSegment.pkMap();
				GlueDataObject.delete(cmdContext, AlignedSegment.class, pkMap, false);
			}

		}
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
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					return GlueDataObject.query(cmdContext, Alignment.class, new SelectQuery(Alignment.class))
							.stream()
							.map(almt -> new CompletionSuggestion(almt.getName(), true))
							.collect(Collectors.toList());
				}
			});
			registerEnumLookup(MERGE_STRATEGY, SegmentMergeStrategy.class);
		}
	}

	
}
