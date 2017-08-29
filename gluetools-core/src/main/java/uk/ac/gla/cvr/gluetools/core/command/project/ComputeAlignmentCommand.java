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
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeUnconstrained;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

// could make this a module, but it seems quite fundamental to the model, 
// and doesn't seem to require much if any configuration.

@CommandClass(
		commandWords={"compute", "alignment"}, 
		description = "Align member segments using an aligner module", 
		docoptUsages = {"<alignmentName> <alignerModuleName> [-w <whereClause>] [-b <batchSize>]"}, 
		docoptOptions = {
				"-w <whereClause>, --whereClause <whereClause>  Qualify which members will be re-aligned",
				"-b <batchSize>, --batchSize <batchSize>        Re-alignment batch size"},
		metaTags={CmdMeta.updatesDatabase},
		furtherHelp = "Computes the aligned segments of certain members of the specified alignment, using a given aligner module. "+
		"If <whereClause> is not specified, all members of the alignment are re-aligned. "+
		"Alignment members are aligned in batches, according to <batchSize>. Default <batchSize> is 50."+
		" Example: compute alignment AL1 blastAligner -w \"sequence.genotype = 4\""
)
public class ComputeAlignmentCommand extends ProjectModeCommand<ComputeAlignmentCommand.ComputeAlignmentResult> {

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
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(50);
	}
	

	@Override
	public ComputeAlignmentResult execute(CommandContext cmdContext) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Aligner<?, ?> alignerModule = Aligner.getAligner(cmdContext, alignerModuleName);
		if(alignment.isConstrained()) {
			if(	(!(alignerModule instanceof SupportsComputeConstrained)) ||
					!((SupportsComputeConstrained) alignerModule).supportsComputeConstrained()) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, 
						"Aligner module '"+alignerModuleName+"' does not support computing of constrained alignments.");
			}
		} else {
			if(!(alignerModule instanceof SupportsComputeUnconstrained)) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, 
						"Aligner module '"+alignerModuleName+"' does not support computing of unconstrained alignments.");
			}
			if(whereClause != null) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, 
						"Cannot use --whereClause when computing an unconstrained alignment");
			}
		}
		// enter the alignment command mode to get the reference sequence name 

		List<Map<String, Object>> resultListOfMaps;
		if(alignment.isConstrained()) {
			GlueLogger.getGlueLogger().finest("Searching for members to align");
			// enter the alignment command mode to get the member ID maps selected by the where clause
			List<Map<String, Object>> memberIDs = AlignmentComputationUtils.getMemberSequenceIdMaps(cmdContext, alignmentName, whereClause);
			GlueLogger.getGlueLogger().finest("Found "+memberIDs.size()+" members to align");
			ArrayList<Map<String, Object>> memberIDsList = new ArrayList<Map<String,Object>>(memberIDs);
			@SuppressWarnings("unchecked")
			SupportsComputeConstrained supportsComputeConstrained = ((SupportsComputeConstrained) alignerModule);
			resultListOfMaps = getComputeConstrainedResults(cmdContext, supportsComputeConstrained, memberIDsList, alignment.getRefSequence().getName());
		} else {
			@SuppressWarnings("unchecked")
			SupportsComputeUnconstrained supportsComputeUnconstrained = ((SupportsComputeUnconstrained) alignerModule);
			resultListOfMaps = getComputeUnconstrainedResults(cmdContext, supportsComputeUnconstrained, alignmentName);
		}
		
		return new ComputeAlignmentResult(resultListOfMaps);
	}


	private List<Map<String, Object>> getComputeUnconstrainedResults(
			CommandContext cmdContext,
			SupportsComputeUnconstrained supportsComputeUnconstrained,
			String alignmentName) {
		Map<Map<String,String>, List<QueryAlignedSegment>>
			alignmentRows = supportsComputeUnconstrained.computeUnconstrained(cmdContext, alignmentName);
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		alignmentRows.forEach((memberPkMap, memberAlignedSegments) -> {
			results.add(AlignmentComputationUtils.applyMemberAlignedSegments(cmdContext, memberPkMap, memberAlignedSegments));
		});
		return results;
	}


	private <R extends AlignerResult, C extends Command<R>> List<Map<String, Object>> getComputeConstrainedResults(
			CommandContext cmdContext, SupportsComputeConstrained supportsComputeConstrained, ArrayList<Map<String, Object>> memberIDs, String refName) {
		@SuppressWarnings("unchecked")
		Class<C> alignCommandClass = (Class<C>) supportsComputeConstrained.getComputeConstrainedCommandClass();
		
		int membersAligned = 0;
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		while(membersAligned < memberIDs.size()) {
			int nextMembersAligned = Math.min(membersAligned+batchSize, memberIDs.size());
			List<Map<String, Object>> membersBatch = memberIDs.subList(membersAligned, nextMembersAligned);
			getBatchResult(cmdContext, membersBatch, refName, alignCommandClass, resultListOfMaps);
			membersAligned = nextMembersAligned;
			GlueLogger.getGlueLogger().finest("Aligned "+membersAligned+" members");
			cmdContext.newObjectContext();
		}
		return resultListOfMaps;
	}


	private <R extends AlignerResult, C extends Command<R>> void getBatchResult(
			CommandContext cmdContext,
			List<Map<String, Object>> memberIDs, String refName,
			Class<C> alignCommandClass,
			List<Map<String, Object>> resultListOfMaps) {
		Map<String,String> queryIdToNucleotides = AlignmentComputationUtils.getMembersNtMap(cmdContext, memberIDs);
		R alignerResult = getAlignerResult(cmdContext, alignCommandClass, refName, queryIdToNucleotides);
		Map<String, List<QueryAlignedSegment>> queryIdToAlignedSegments = alignerResult.getQueryIdToAlignedSegments();
		for(Map<String, Object> memberIDmap: memberIDs) {
			String memberSourceName = (String) memberIDmap.get(AlignmentMember.SOURCE_NAME_PATH);
			String memberSeqId = (String) memberIDmap.get(AlignmentMember.SEQUENCE_ID_PATH);
			String memberFastaId = AlignmentComputationUtils.constructQueryId(memberSourceName, memberSeqId);
			List<QueryAlignedSegment> memberAlignedSegments = queryIdToAlignedSegments.get(memberFastaId);
			Map<String, Object> memberResultMap = AlignmentComputationUtils.applyMemberAlignedSegments(cmdContext,alignmentName,
					memberSourceName, memberSeqId, memberAlignedSegments);
			resultListOfMaps.add(memberResultMap);
		}
	}

	private <R extends AlignerResult, C extends Command<R>> R getAlignerResult(
			CommandContext cmdContext, Class<C> alignCommandClass, 
			String refName, Map<String, String> queryIdToNucleotides) {
		R alignerResult;
		try(ModeCloser moduleMode = cmdContext.pushCommandMode("module", alignerModuleName)) {
			CommandBuilder<R, C> alignCmdBuilder = cmdContext.cmdBuilder(alignCommandClass)
				.set(AlignCommand.REFERENCE_NAME, refName);
			CommandArray seqArrayBuilder = alignCmdBuilder
				.setArray(AlignCommand.SEQUENCE);
			queryIdToNucleotides.forEach((queryId, nts) ->
			{
				seqArrayBuilder.addObject()
					.set(AlignCommand.QUERY_ID, queryId)
					.set(AlignCommand.NUCLEOTIDES, nts);
			});
			alignerResult = alignCmdBuilder.execute();
		}
		return alignerResult;
	}
	
	

	
	public static class ComputeAlignmentResult extends TableResult {
		public static final String REMOVED_SEGMENTS = "removedSegments";
		public static final String ADDED_SEGMENTS = "addedSegments";
		
		protected ComputeAlignmentResult(List<Map<String, Object>> listOfMaps) {
			super("computeAlignmentResult",  
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
