package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"project"},
	docoptUsages={"<projectName>"},
	description="Enter a project command mode")
@EnterModeCommandClass(
		commandModeClass = ProjectMode.class)
public class ProjectCommand extends RootModeCommand  {

	private String projectName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Project project = GlueDataObject.lookup(objContext, Project.class, Project.pkMap(projectName));
		cmdContext.pushCommandMode(new ProjectMode(cmdContext, this, project));
		return CommandResult.OK;
	}
	
	@CompleterClass
	public static class Completer extends ProjectNameCompleter {}

}
