package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeConstrained;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsExtendUnconstrained;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@CommandClass(
		commandWords={"extend", "alignment"}, 
		description = "Extend alignment to recompute certain members", 
		docoptUsages = {"<alignmentName> <alignerModuleName> -w <whereClause> [-b <batchSize>]"}, 
		docoptOptions = {
				"-w <whereClause>, --whereClause <whereClause>  Qualify which members will be re-aligned",
				"-b <batchSize>, --batchSize <batchSize>        Re-alignment batch size"},
		metaTags={CmdMeta.updatesDatabase},
		furtherHelp = "(Re-)computes the aligned segments of certain members of the specified unconstrained alignment, "+
		"using a given aligner module. The specified member rows are (re-)computed using the existing unconstrained "+
		"alignment as part of the input. The existing alignment may be updated as a result, in order to introduce gaps "+
		"to accommodate the recomputed segments, but is otherwise unaltered. "+
		"Alignment members are aligned in batches, according to <batchSize>. Default <batchSize> is 50."+
		" Example: exted alignment AL1 mafftAligner -w \"sequence.genotype = 4\""
)
public class ExtendAlignmentCommand extends ProjectModeCommand<ExtendAlignmentCommand.ExtendAlignmentResult> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String BATCH_SIZE = "batchSize";
	
	private String alignmentName;
	private String alignerModuleName;
	private Expression whereClause;
	private int batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, true);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, true);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(50);
	}
	

	@Override
	public ExtendAlignmentResult execute(CommandContext cmdContext) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Aligner<?, ?> alignerModule = Aligner.getAligner(cmdContext, alignerModuleName);
		if(alignment.isConstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, 
					"Command is currently limited to unconstrained alignments but alignment '"+alignmentName+"' is constrained.");
		}
		if(	(!(alignerModule instanceof SupportsExtendUnconstrained)) ||
				!((SupportsExtendUnconstrained) alignerModule).supportsExtendUnconstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, 
					"Aligner module '"+alignerModuleName+"' does not support extending of unconstrained alignments.");
		}
		
		GlueLogger.getGlueLogger().finest("Searching for members to align");
		// enter the alignment command mode to get the member ID maps selected by the where clause
		List<Map<String, Object>> memberIDs = AlignmentComputationUtils.getMemberSequenceIdMaps(cmdContext, alignmentName, whereClause);
		GlueLogger.getGlueLogger().finest("Found "+memberIDs.size()+" members to align");
		// get the original data for the reference sequence
		// modify the aligned segments and generate alignment results results for each selected member.
		List<Map<String, Object>> resultListOfMaps = 
				getComputeConstrainedResults(cmdContext, new ArrayList<Map<String,Object>>(memberIDs), alignment.getRefSequence().getName());
		return new ExtendAlignmentResult(resultListOfMaps);
	}


	private <R extends AlignerResult, C extends Command<R>> List<Map<String, Object>> getComputeConstrainedResults(
			CommandContext cmdContext, ArrayList<Map<String, Object>> memberIDs, String refName) {
		// get the align command's class for the module.
		// Look up the module by name, check it is an aligner, and get its align command class.
		Aligner<?, ?> alignerModule = Aligner.getAligner(cmdContext, alignerModuleName);

		int membersAligned = 0;
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		while(membersAligned < memberIDs.size()) {
			int nextMembersAligned = Math.min(membersAligned+batchSize, memberIDs.size());
			List<Map<String, Object>> membersBatch = memberIDs.subList(membersAligned, nextMembersAligned);
			getBatchResult(cmdContext, membersBatch, refName, alignerModule, resultListOfMaps);
			membersAligned = nextMembersAligned;
			GlueLogger.getGlueLogger().finest("Aligned "+membersAligned+" members");
			cmdContext.newObjectContext();
		}
		return resultListOfMaps;
	}


	private <R extends AlignerResult> void getBatchResult(
			CommandContext cmdContext,
			List<Map<String, Object>> memberIDs, String refName,
			Aligner<R, ?> aligner,
			List<Map<String, Object>> resultListOfMaps) {
		Map<String,String> queryIdToNucleotides = AlignmentComputationUtils.getMembersNtMap(cmdContext, memberIDs);
		R alignerResult = getAlignerResult(cmdContext, aligner, refName, queryIdToNucleotides);
		Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments = alignerResult.getQueryIdToAlignedSegments();
		for(Map<String, Object> memberIDmap: memberIDs) {
			String memberSourceName = (String) memberIDmap.get(AlignmentMember.SOURCE_NAME_PATH);
			String memberSeqId = (String) memberIDmap.get(AlignmentMember.SEQUENCE_ID_PATH);
			String memberFastaId = AlignmentComputationUtils.constructQueryId(memberSourceName, memberSeqId);
			List<QueryAlignedSegment> memberAlignedSegments = queryIdToAlignedSegments.get(memberFastaId);
			Map<String, Object> memberResultMap = AlignmentComputationUtils.applyMemberAlignedSegments(cmdContext,
					alignmentName, memberSourceName, memberSeqId, memberAlignedSegments);
			resultListOfMaps.add(memberResultMap);
		}
	}

	private <R extends AlignerResult> R getAlignerResult(
			CommandContext cmdContext, Aligner<R,?> aligner, 
			String refName, Map<String, String> queryIdToNucleotides) {
		R alignerResult = null;
		
		// TODO!
		
		return alignerResult;
	}
	
	



	public static class ExtendAlignmentResult extends TableResult {
		public static final String REMOVED_SEGMENTS = "removedSegments";
		public static final String ADDED_SEGMENTS = "addedSegments";
		
		protected ExtendAlignmentResult(List<Map<String, Object>> listOfMaps) {
			super("extendAlignmentResult",  
					Arrays.asList(
							AlignmentMember.SOURCE_NAME_PATH, 
							AlignmentMember.SEQUENCE_ID_PATH, 
							REMOVED_SEGMENTS,
							ADDED_SEGMENTS),
					listOfMaps);
		}
	}
	
	
	@CompleterClass
	public static class Completer extends AlignmentNameCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignerModuleName", Module.class, Module.NAME_PROPERTY);
		}
	}

	
}
