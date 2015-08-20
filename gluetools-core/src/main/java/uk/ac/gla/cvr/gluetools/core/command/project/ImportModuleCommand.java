package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.ModuleException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"import","module"}, 
	docoptUsages={"<name> <fileName>"},
	description="Create a new module, importing its config from a file") 
public class ImportModuleCommand extends ProjectModeCommand<CreateResult> {

	private String name;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		name = PluginUtils.configureStringProperty(configElem, "name", true);
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Module module = GlueDataObject.create(objContext, Module.class, Module.pkMap(name), false);
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		byte[] config = consoleCmdContext.loadBytes(fileName);
		module.setConfig(config);
		try {
			module.getModulePlugin(cmdContext.getGluetoolsEngine());
		} catch(Exception e) {
			throw new ModuleException(e, Code.CREATE_FROM_FILE_FAILED, fileName);
		}
		cmdContext.commit();
		return new CreateResult(Module.class, 1);
	}

}
