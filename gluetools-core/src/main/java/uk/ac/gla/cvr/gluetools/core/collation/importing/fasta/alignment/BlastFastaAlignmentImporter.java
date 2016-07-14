package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporarySingleSeqBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="blastFastaAlignmentImporter")
public class BlastFastaAlignmentImporter extends FastaNtAlignmentImporter<BlastFastaAlignmentImporter> {


	private BlastRunner blastRunner = new BlastRunner();

	public BlastFastaAlignmentImporter() {
		super();
		addModulePluginCmdClass(ImportCommand.class);
		addModulePluginCmdClass(PreviewCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, "blastRunner");
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
	}

	@Override
	protected List<QueryAlignedSegment> findAlignedSegs(CommandContext cmdContext, Sequence foundSequence, 
			List<QueryAlignedSegment> existingSegs, String alignmentRowNTs, 
			String fastaID) {
		
		// remove alignment gaps so that BLAST does not have to jump them. 
		// But remember the mapping so we can apply it later.
		// In these segments incoming row is the reference, gapless is the query
		List<QueryAlignedSegment> gaplessToIncomingRow = new ArrayList<QueryAlignedSegment>();
		QueryAlignedSegment currentSegment = null;
		StringBuffer alignmentRowNtGapless = new StringBuffer();
		int incomingRowIndex = 1;
		int gaplessIndex = 1;
		for(int i = 0; i < alignmentRowNTs.length(); i++) {
			char alignmentRowChar = alignmentRowNTs.charAt(i);
			if(alignmentRowChar == '-') {
				if(currentSegment != null) {
					currentSegment = null;
				}
			} else {
				alignmentRowNtGapless.append(alignmentRowChar);
				if(currentSegment == null) {
					currentSegment = new QueryAlignedSegment(incomingRowIndex, incomingRowIndex, gaplessIndex, gaplessIndex);
					gaplessToIncomingRow.add(currentSegment);
				} else {
					currentSegment.setRefEnd(incomingRowIndex);
					currentSegment.setQueryEnd(gaplessIndex);
				}
				gaplessIndex++;
			}
			incomingRowIndex++;
		}
		
		byte[] gaplessFastaBytes = FastaUtils.seqIdCompoundsPairToFasta("alignmentRowNTs", 
				alignmentRowNtGapless.toString()).getBytes();

		String foundSequenceNTs = foundSequence.getSequenceObject().getNucleotides(cmdContext);
		BlastDbManager blastDbManager = BlastDbManager.getInstance();

		String uuid = UUID.randomUUID().toString();
		List<BlastResult> blastResults = null;
		try {
			TemporarySingleSeqBlastDB tempBlastDB = 
					blastDbManager.createTempSingleSeqBlastDB(cmdContext, uuid, "glueSequenceRef", foundSequenceNTs);
			blastResults = blastRunner.executeBlast(cmdContext, tempBlastDB, gaplessFastaBytes);
		} finally {
			blastDbManager.removeTempSingleSeqBlastDB(cmdContext, uuid);
		}

		List<QueryAlignedSegment> foundSeqToGapless = new ArrayList<QueryAlignedSegment>();

		Map<String, List<QueryAlignedSegment>> blastResultsToAlignedSegmentsMap = 
				BlastUtils.blastNResultsToAlignedSegmentsMap("glueSequenceRef", blastResults, null);
		List<QueryAlignedSegment> blastAlignedSegments = blastResultsToAlignedSegmentsMap.get("alignmentRowNTs");
		if(blastAlignedSegments != null) {
			for(QueryAlignedSegment queryAlignedSegment: blastAlignedSegments) {
				foundSeqToGapless.add(queryAlignedSegment.invert());
			}
		}
		List<QueryAlignedSegment> foundSeqToIncomingRow = QueryAlignedSegment.translateSegments(foundSeqToGapless, gaplessToIncomingRow);
		return foundSeqToIncomingRow;
	}

	
	@CommandClass( 
			commandWords={"import"}, 
			docoptUsages={"<alignmentName> -f <fileName> [-s <sourceName>]"},
			docoptOptions={
			"-f <fileName>, --fileName <fileName>        FASTA file",
			"-s <sourceName>, --sourceName <sourceName>  Restrict alignment members to a given source"},
			description="Import an unconstrained alignment from a FASTA file", 
			metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
			furtherHelp="The file is loaded from a location relative to the current load/save directory. "+
			"An existing unconstrained alignment will be updated with new members, or a new unconstrained alignment will be created.") 
	public static class ImportCommand extends ModulePluginCommand<FastaAlignmentImporterResult, BlastFastaAlignmentImporter> implements ProvidedProjectModeCommand {

		private String fileName;
		private String alignmentName;
		private String sourceName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
			alignmentName = PluginUtils.configureStringProperty(configElem, "alignmentName", true);
			sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		}
		
		@Override
		protected FastaAlignmentImporterResult execute(CommandContext cmdContext, BlastFastaAlignmentImporter importerPlugin) {
			Alignment alignment = importerPlugin.initAlignment(cmdContext, alignmentName);
			return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName, alignment, sourceName);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerVariableInstantiator("alignmentName", new VariableInstantiator() {
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
				registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
				registerPathLookup("fileName", false);
			}
		}

	}

	@CommandClass( 
			commandWords={"preview"}, 
			docoptUsages={"-f <fileName> [-s <sourceName>]"},
			docoptOptions={
			"-f <fileName>, --fileName <fileName>        FASTA file",
			"-s <sourceName>, --sourceName <sourceName>  Restrict alignment members to a given source"},
			description="Preview import of an unconstrained alignment from a FASTA file", 
			metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
			furtherHelp="The file is loaded from a location relative to the current load/save directory. ") 
	public static class PreviewCommand extends ModulePluginCommand<FastaAlignmentImporterResult, BlastFastaAlignmentImporter> implements ProvidedProjectModeCommand {

		private String fileName;
		private String sourceName;
		
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			super.configure(pluginConfigContext, configElem);
			fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
			sourceName = PluginUtils.configureStringProperty(configElem, "sourceName", false);
		}
		
		@Override
		protected FastaAlignmentImporterResult execute(CommandContext cmdContext, BlastFastaAlignmentImporter importerPlugin) {
			return importerPlugin.doPreview((ConsoleCommandContext) cmdContext, fileName, sourceName);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerVariableInstantiator("alignmentName", new VariableInstantiator() {
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
				registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
				registerPathLookup("fileName", false);
			}
		}

	}

	
	
	
	
}
