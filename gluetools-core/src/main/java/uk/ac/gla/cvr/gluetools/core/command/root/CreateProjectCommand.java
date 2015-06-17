package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CreateCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectCommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="create-project")
@CommandClass(description="Create a new project", 
	docoptUsages={"<displayName>"}) 
public class CreateProjectCommand extends Command {

	private String displayName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		displayName = PluginUtils.configureString(configElem, "displayName/text()", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		Project newProject = objContext.newObject(Project.class);
		newProject.setDisplayName(displayName);
		objContext.commitChanges();
		ObjectId objectId = newProject.getObjectId();
		return new CreateCommandResult(objectId);
	}

}
