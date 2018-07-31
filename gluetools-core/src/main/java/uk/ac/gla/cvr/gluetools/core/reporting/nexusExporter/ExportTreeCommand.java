package uk.ac.gla.cvr.gluetools.core.reporting.nexusExporter;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"export", "tree"}, 
		description = "Export a NEXUS tree file based on an alignment phylogeny", 
		docoptUsages = { "<almtName> -f <fileName>"},
		docoptOptions = { 
				"-f <fileName>, --fileName <fileName>           Output to file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)

public class ExportTreeCommand extends ModulePluginCommand<OkResult, NexusExporter> {

	public static final String ALIGNMENT_NAME = "almtName";
	public static final String FILE_NAME = "fileName";
	
	private String almtName;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.almtName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, NexusExporter nexusExporter) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName));
		String nexusString = nexusExporter.exportNexus(cmdContext, alignment);
		((ConsoleCommandContext) cmdContext).saveBytes(this.fileName, nexusString.getBytes());
		return new OkResult();
	}


	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			registerDataObjectNameLookup("almtName", Alignment.class, Alignment.NAME_PROPERTY);
			registerPathLookup("fileName", false);
		}
	}
	
}
