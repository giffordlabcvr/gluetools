package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;


@CommandClass(
	commandWords={"list", "alignment"}, 
	docoptUsages={""},
	description="List alignments") 
public class ListAlignmentCommand extends ProjectModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, Alignment.class, new SelectQuery(Alignment.class));
	}

}