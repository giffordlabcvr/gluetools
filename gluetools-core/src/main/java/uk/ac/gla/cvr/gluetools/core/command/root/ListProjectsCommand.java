package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;


@CommandClass( 
	commandWords={"list", "projects"}, 
	docoptUsages={""},
	description="List all projects") 
public class ListProjectsCommand extends RootModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, Project.class, new SelectQuery(Project.class));
	}

}
