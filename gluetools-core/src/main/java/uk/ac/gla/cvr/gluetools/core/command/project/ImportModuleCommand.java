package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"import","module"}, 
	docoptUsages={"[-r] <name> <fileName>"},
	docoptOptions={"-r, --reload  If module exists, reload its config"},
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase},
	description="Create a new module, importing config from a file") 
public class ImportModuleCommand extends ProjectModeCommand<OkResult> {

	private String name;
	private String fileName;
	private Boolean reload;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		name = PluginUtils.configureStringProperty(configElem, "name", true);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		reload = PluginUtils.configureBooleanProperty(configElem, "reload", true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Module module = null;
		boolean moduleExisted = false;
		if(reload) {
			module = GlueDataObject.lookup(objContext, Module.class, Module.pkMap(name), true);
		}
		if(module != null) {
			moduleExisted = true;
		} else {
			module = GlueDataObject.create(objContext, Module.class, Module.pkMap(name), false);
		}
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		byte[] config = consoleCmdContext.loadBytes(fileName);
		module.setConfig(config);
		try {
			module.getModulePlugin(cmdContext.getGluetoolsEngine());
		} catch(Exception e) {
			throw new ModuleException(Code.CREATE_FROM_FILE_FAILED, fileName, e.getMessage());
		}
		cmdContext.commit();
		if(moduleExisted) {
			return new UpdateResult(Module.class, 1);
		} else {
			return new CreateResult(Module.class, 1);
		}
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}

	
	
	
}
