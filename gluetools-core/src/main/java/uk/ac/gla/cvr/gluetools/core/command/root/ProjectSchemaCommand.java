package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.ProjectSchemaMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"schema", "project"},
	docoptUsages={"<projectName>"},
	description="Enter command mode to manage a project's schema") 
public class ProjectSchemaCommand extends RootModeCommand implements EnterModeCommand {

	private String projectName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Project project = GlueDataObject.lookup(objContext, Project.class, Project.pkMap(projectName));
		cmdContext.pushCommandMode(new ProjectSchemaMode(cmdContext, project));
		return CommandResult.OK;
	}
	
	@CompleterClass
	public static class Completer extends ProjectNameCompleter {}


}
