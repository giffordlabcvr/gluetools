package uk.ac.gla.cvr.gluetools.core.command.root;

import java.util.Optional;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="create-project")
@CommandClass(description="Create a new project", 
	docoptUsages={"<projectName> [<description>]"}) 
public class CreateProjectCommand extends RootModeCommand {

	private String projectName;
	private Optional<String> description;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		projectName = PluginUtils.configureStringProperty(configElem, "projectName", true);
		description = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "description", false));
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		Project newProject = GlueDataObject.create(objContext, Project.class, Project.pkMap(projectName));
		description.ifPresent(newProject::setDescription);
		ServerRuntime projectRuntime = 
				ModelBuilder.createProjectModel(cmdContext.peekCommandMode().getServerRuntime(), newProject);
		projectRuntime.shutdown();
		return new CreateCommandResult(newProject.getObjectId());
	}

}
