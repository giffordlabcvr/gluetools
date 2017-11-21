package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleMode;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModuleModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"module"},
	docoptUsages={"<moduleName>"},
	description="Enter command mode for a module") 
@EnterModeCommandClass(
		commandFactoryClass = ModuleModeCommandFactory.class)
public class ModuleCommand extends ProjectModeCommand<OkResult>  {

	private String moduleName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		moduleName = PluginUtils.configureStringProperty(configElem, "moduleName", true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(moduleName));
		cmdContext.pushCommandMode(new ModuleMode(cmdContext, getProjectMode(cmdContext).getProject(), this, module.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends ModuleNameCompleter {}
	

}
