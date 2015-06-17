package uk.ac.gla.cvr.gluetools.core.command.root;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.ListCommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="list-projects")
@CommandClass(description="List all projects", 
	docoptUsages={""}) 
public class ListProjectsCommand extends Command {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getGluetoolsEngine().getCayenneObjectContext();
		List<?> queryResult = objContext.performQuery(new SelectQuery(Project.class));
		List<Project> projects = queryResult.stream().map(obj -> (Project) obj).collect(Collectors.toList());
		return new ListCommandResult<Project>(Project.class, projects);
	}

}
