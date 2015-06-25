package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"module"},
	docoptUsages={"<moduleName>"},
	description="Enter command mode to manage a module") 
public class ModuleCommand extends ProjectModeCommand implements EnterModeCommand {

	private String moduleName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		moduleName = PluginUtils.configureStringProperty(configElem, "moduleName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		// check module exists.
		GlueDataObject.lookup(objContext, Module.class, Module.pkMap(moduleName));
		cmdContext.pushCommandMode(new ModuleMode(cmdContext, moduleName));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends ModuleNameCompleter {}

}
