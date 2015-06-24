package uk.ac.gla.cvr.gluetools.core.command.project.module;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;


@CommandClass( 
	commandWords={"list", "modules"}, 
	docoptUsages={""},
	description="List modules in the current project") 
public class ListModulesCommand extends ProjectModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, Module.class, new SelectQuery(Module.class));
	}

}
