package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "module"}, 
	metaTags={CmdMeta.updatesDatabase},
	docoptUsages={"<moduleName>"},
	description="Delete a module") 
public class DeleteModuleCommand extends ProjectModeCommand<DeleteResult> {

	private String moduleName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		moduleName = PluginUtils.configureStringProperty(configElem, "moduleName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		DeleteResult result = GlueDataObject.delete(objContext, Module.class, Module.pkMap(moduleName));
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends ModuleNameCompleter {}

}
