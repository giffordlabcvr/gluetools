package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(commandWords={"save", "configuration"},
	docoptUsages="<fileName>",
	description = "Save module configuration to a file", 
	docCategory="General module commands",
	metaTags = { CmdMeta.consoleOnly } )
public class ModuleSaveConfigurationCommand extends ModuleDocumentCommand<OkResult> {

	private static final String FILE_NAME = "fileName";
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}

	protected OkResult processDocument(CommandContext cmdContext,
			Module module, Document modulePluginDoc) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		consoleCmdContext.saveBytes(fileName, module.getConfig());
		return new OkResult();
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}

}
