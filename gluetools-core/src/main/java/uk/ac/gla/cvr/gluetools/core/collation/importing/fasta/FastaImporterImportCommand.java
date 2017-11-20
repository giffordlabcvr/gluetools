package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"import"}, 
		docoptUsages={"-f <fileName>"},
		docoptOptions={
			"-f <fileName>, --fileName <fileName>  FASTA file"},
		description="Import sequences from a FASTA file", 
		metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase },
		furtherHelp="The file is loaded from a location relative to the current load/save directory.") 
public class FastaImporterImportCommand extends ModulePluginCommand<CreateResult, FastaImporter> implements ProvidedProjectModeCommand {

	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
	}
	
	@Override
	protected CreateResult execute(CommandContext cmdContext, FastaImporter importerPlugin) {
		return importerPlugin.doImport((ConsoleCommandContext) cmdContext, fileName);
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}

}