package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.sequenceGroup.SequenceGroup;


@CommandClass(
	commandWords={"list", "group"}, 
	docoptUsages={""},
	description="List sequence groups") 
public class ListGroupCommand extends ProjectModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, SequenceGroup.class, new SelectQuery(SequenceGroup.class));
	}

}
