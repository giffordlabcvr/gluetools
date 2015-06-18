package uk.ac.gla.cvr.gluetools.core.command.project.importer;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.Importer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="create-importer")
@CommandClass(description="Create a new sequence importer in this project", 
	docoptUsages={"<name> -f <configFile>"},
	docoptOptions={"-f <file>, --file <file>  Importer configuration file"}) 
public class CreateImporterCommand extends ProjectModeCommand {

	private String name;
	private String file;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		name = PluginUtils.configureString(configElem, "name/text()", true);
		file = PluginUtils.configureString(configElem, "file/text()", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		Importer importer = GlueDataObject.create(objContext, Importer.class, Importer.pkMap(getProjectName(), name));
		importer.setProject(getProject(objContext));
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		byte[] config = consoleCmdContext.loadBytes(file);
		importer.setConfig(config);
		importer.getImporterPlugin(cmdContext.getGluetoolsEngine().createPluginConfigContext());
		objContext.commitChanges();
		return new CreateCommandResult(importer.getObjectId());
	}

}
