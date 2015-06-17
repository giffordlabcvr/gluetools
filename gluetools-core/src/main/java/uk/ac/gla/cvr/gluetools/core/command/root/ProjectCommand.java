package uk.ac.gla.cvr.gluetools.core.command.root;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectCommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException;
import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="project")
@CommandClass(description="Enter a project command mode", 
	docoptUsages={"<projectId>"}) 
public class ProjectCommand extends Command {

	private String projectId;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		projectId = PluginUtils.configureString(configElem, "projectId/text()", true);
	}

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		Project.lookupProject(objContext, projectId);
		cmdContext.pushCommandMode(new ProjectCommandMode(projectId));
		return CommandResult.OK;
	}

}
