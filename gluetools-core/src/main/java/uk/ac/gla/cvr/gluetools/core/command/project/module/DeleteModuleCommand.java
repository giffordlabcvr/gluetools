package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="delete-module")
@CommandClass(description="Delete a module", 
	docoptUsages={"<moduleName>"}) 
public class DeleteModuleCommand extends ProjectModeCommand {

	private String moduleName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		moduleName = PluginUtils.configureStringProperty(configElem, "moduleName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		GlueDataObject.delete(objContext, Module.class, Module.pkMap(getProjectName(), moduleName));
		objContext.commitChanges();
		return CommandResult.OK;
	}


}
