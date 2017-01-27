package uk.ac.gla.cvr.gluetools.core.command.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsExtendUnconstrained;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@CommandClass(
		commandWords={"extend", "alignment"}, 
		description = "Extend alignment to recompute certain members", 
		docoptUsages = {"<alignmentName> <alignerModuleName> -w <whereClause> [-b <batchSize>] [-d <dataDir>]"}, 
		docoptOptions = {
				"-w <whereClause>, --whereClause <whereClause>  Qualify which members will be re-aligned",
				"-b <batchSize>, --batchSize <batchSize>        Re-alignment batch size",
				"-d <dataDir>, --dataDir <dataDir>              Directory to save temporary data in"},
		metaTags={CmdMeta.updatesDatabase},
		furtherHelp = "(Re-)computes the aligned segments of certain members of the specified unconstrained alignment, "+
		"using a given aligner module. The specified member rows are (re-)computed using the existing unconstrained "+
		"alignment as part of the input. The existing alignment will not be updated as a result, no gaps will be added.\n"+
		"Alignment members are aligned in batches, according to <batchSize>. Default <batchSize> is 50."+
		" Example: exted alignment AL1 mafftAligner -w \"sequence.genotype = 4\""
)
public class ExtendAlignmentCommand extends ProjectModeCommand<ExtendAlignmentCommand.ExtendAlignmentResult> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String BATCH_SIZE = "batchSize";
	public static final String DATA_DIR = "dataDir";
	
	private String alignmentName;
	private String alignerModuleName;
	private String dataDirString;
	private Expression whereClause;
	private int batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, true);
		whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, true);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(50);
		dataDirString = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
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
				!((SupportsExtendUnconstrained<?>) alignerModule).supportsExtendUnconstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, 
					"Aligner module '"+alignerModuleName+"' does not support extending of unconstrained alignments.");
		}
		Expression recomputedExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignmentName)
				.andExp(whereClause);
		SelectQuery recomputedSelectQuery = new SelectQuery(AlignmentMember.class, 
				recomputedExp);
		List<AlignmentMember> recomputedMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, recomputedSelectQuery);
		Expression existingExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignmentName)
				.andExp(whereClause.notExp());
		SelectQuery existingSelectQuery = new SelectQuery(AlignmentMember.class, existingExp);
		List<AlignmentMember> existingMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, existingSelectQuery);
		if(	existingMembers.isEmpty() ) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, 
					"Where clause excluded all existing members.");
		}
		
		// get the original data for the reference sequence
		// modify the aligned segments and generate alignment results results for each selected member.
		List<Map<String, Object>> resultListOfMaps = getExtendUnconstrainedResults(cmdContext, 
						(SupportsExtendUnconstrained<?>) alignerModule,		
						existingMembers,
						recomputedMembers);
		
		return new ExtendAlignmentResult(resultListOfMaps);
	}


	private <R extends AlignerResult, C extends Command<R>> List<Map<String, Object>> getExtendUnconstrainedResults(
			CommandContext cmdContext,
			SupportsExtendUnconstrained<?> alignerModule, 
			List<AlignmentMember> existingMembers, 
			List<AlignmentMember> recomputedMembers) {
		
		List<Map<String,String>> existingMembersPkMaps = new ArrayList<Map<String,String>>();
		existingMembers.forEach(member -> {
			existingMembersPkMaps.add(member.pkMap());
		});
		List<Map<String,String>> recomputedMembersPkMaps = new ArrayList<Map<String,String>>();
		recomputedMembers.forEach(member -> {
			recomputedMembersPkMaps.add(member.pkMap());
		});
		
		List<Map<String, Object>> resultListOfMaps = new ArrayList<Map<String, Object>>();
		
		int membersAligned = 0;
		while(membersAligned < recomputedMembersPkMaps.size()) {
			int nextMembersAligned = Math.min(membersAligned+batchSize, recomputedMembersPkMaps.size());
			List<Map<String,String>> membersBatchPkMaps = recomputedMembersPkMaps.subList(membersAligned, nextMembersAligned);
			processBatch(cmdContext, alignerModule, existingMembersPkMaps, membersBatchPkMaps, resultListOfMaps);
			membersAligned = nextMembersAligned;
			GlueLogger.getGlueLogger().finest("Recomputed "+membersAligned+" members");
			cmdContext.newObjectContext();
		}

		return resultListOfMaps;
	}


	private void processBatch(CommandContext cmdContext,
			SupportsExtendUnconstrained<?> alignerModule,
			List<Map<String,String>> existingMembersPkMaps,
			List<Map<String,String>> recomputedMembersPkMaps,
			List<Map<String, Object>> resultListOfMaps) {

		File dataDir = null;
		if(dataDirString != null) {
			if(!(cmdContext instanceof ConsoleCommandContext)) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "The <dataDir> option is only available from the console");
			}
			ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
			dataDir = consoleCommandContext.fileStringToFile(dataDirString);
			consoleCommandContext.mkdirs(dataDir);
		}
		
		Map<Map<String,String>, List<QueryAlignedSegment>> pkMapToNewSegments = 
				alignerModule.extendUnconstrained(cmdContext,  
						alignmentName, existingMembersPkMaps, recomputedMembersPkMaps, dataDir);
		
		pkMapToNewSegments.forEach( (pkMap, qaSegs) -> {
			if(recomputedMembersPkMaps.contains(pkMap)) {
				Map<String, Object> resultRow = 
						AlignmentComputationUtils.applyMemberAlignedSegments(cmdContext, pkMap, qaSegs);
				resultListOfMaps.add(resultRow);
			}
		});
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
			registerPathLookup("dataDir", true);
		}
	}

	
}
