package uk.ac.gla.cvr.gluetools.core.command.project.module;

import java.util.Optional;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(commandWords={"load", "configuration"},
	docoptUsages="<fileName> [-r]",
	docoptOptions={
		"-r, --loadResources  Also load dependent resources",
	},
	docCategory="General module commands",
	description = "Load module configuration from a file", 
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase } )
public class ModuleLoadConfigurationCommand extends ModuleDocumentCommand<UpdateResult> {

	private static final String FILE_NAME = "fileName";
	private static final String LOAD_RESOURCES = "loadResources";
	private String fileName;
	private boolean loadResources;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.loadResources = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, LOAD_RESOURCES, true)).orElse(false);
	}

	// do the commit here rather than implementing the ModuleUpdateDocumentCommand marker interface.
	@Override
	protected UpdateResult processDocument(CommandContext cmdContext,
			Module module, Document modulePluginDoc) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		module.loadConfig(consoleCmdContext, fileName, loadResources);
		consoleCmdContext.commit();
		return new UpdateResult(Module.class, 1);
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}


}
