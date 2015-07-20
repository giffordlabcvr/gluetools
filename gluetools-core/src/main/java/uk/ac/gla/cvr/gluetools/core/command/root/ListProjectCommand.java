package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;


@CommandClass( 
	commandWords={"list", "project"}, 
	docoptUsages={""},
	description="List all projects") 
public class ListProjectCommand extends RootModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, Project.class, new SelectQuery(Project.class));
	}

}
