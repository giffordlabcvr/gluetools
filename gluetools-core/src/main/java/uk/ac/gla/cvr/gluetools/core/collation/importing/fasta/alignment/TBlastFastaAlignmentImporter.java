package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleProvidedCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;

@PluginClass(elemName="tBlastFastaAlignmentImporter")
public class TBlastFastaAlignmentImporter extends BaseFastaAlignmentImporter<TBlastFastaAlignmentImporter> {


	private BlastRunner blastRunner = new BlastRunner();

	public TBlastFastaAlignmentImporter() {
		super();
		addProvidedCmdClass(ShowImporterCommand.class);
		addProvidedCmdClass(ImportCommand.class);
		addProvidedCmdClass(ConfigureImporterCommand.class);
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
	public FastaAlignmentImporterResult doImport(
			ConsoleCommandContext cmdContext, String fileName,
			String alignmentName, String sourceName) {
		return null;
	}

	@Override
	protected List<QueryAlignedSegment> findAlignedSegs(
			CommandContext cmdContext, Sequence foundSequence,
			List<QueryAlignedSegment> existingSegs, String fastaAlignmentNTs,
			String fastaID) {
		// TODO Auto-generated method stub
		return null;
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
	public static class ImportCommand extends ModuleProvidedCommand<FastaAlignmentImporterResult, TBlastFastaAlignmentImporter> implements ProvidedProjectModeCommand {

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
		protected FastaAlignmentImporterResult execute(CommandContext cmdContext, TBlastFastaAlignmentImporter importerPlugin) {
			return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName, alignmentName, sourceName);
		}
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerDataObjectNameLookup("aligmentName", Alignment.class, Alignment.NAME_PROPERTY);
				registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
				registerPathLookup("fileName", false);
			}
		}

	}
	
	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this importer") 
	public static class ShowImporterCommand extends ShowConfigCommand<TBlastFastaAlignmentImporter> {}

	
	@SimpleConfigureCommandClass(
			propertyNames={IGNORE_REGEX_MATCH_FAILURES, IGNORE_MISSING_SEQUENCES, 
					UPDATE_EXISTING_MEMBERS, UPDATE_EXISTING_ALIGNMENT}
	)
	public static class ConfigureImporterCommand extends SimpleConfigureCommand<TBlastFastaAlignmentImporter> {}


	
}
