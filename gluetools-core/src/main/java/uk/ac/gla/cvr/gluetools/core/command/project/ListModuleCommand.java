package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;


@CommandClass( 
	commandWords={"list", "module"}, 
	docoptUsages={""},
	description="List modules") 
public class ListModuleCommand extends ProjectModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, Module.class, new SelectQuery(Module.class));
	}

}
