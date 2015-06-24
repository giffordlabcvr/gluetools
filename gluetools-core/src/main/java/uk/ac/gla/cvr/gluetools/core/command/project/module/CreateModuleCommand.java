package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","module"}, 
	docoptUsages={"<name> -f <configFile>"},
	docoptOptions={"-f <file>, --file <file>  Module configuration file"},
	description="Create a new module in this project") 
public class CreateModuleCommand extends ProjectModeCommand {

	private String name;
	private String file;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		name = PluginUtils.configureStringProperty(configElem, "name", true);
		file = PluginUtils.configureStringProperty(configElem, "file", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Module module = GlueDataObject.create(objContext, Module.class, Module.pkMap(name));
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		byte[] config = consoleCmdContext.loadBytes(file);
		module.setConfig(config);
		try {
			module.getModulePlugin(cmdContext.getGluetoolsEngine().createPluginConfigContext());
		} catch(Exception e) {
			throw new ModuleException(e, Code.CREATE_FROM_FILE_FAILED, file);
		}
		return new CreateCommandResult(module.getObjectId());
	}

}
