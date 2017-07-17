package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;

@CommandClass(
		commandWords={"new-context"},
		docoptUsages={""},
		docoptOptions={},
		description="Refresh the current command context with a new object context",
		furtherHelp="This is useful to release any objects in the current object context for garbage collection. "+
		"This command should be used periodically during for example large sequence table updates. "+
		"Note that any uncommitted changes in the current context will be lost, so this should normally be preceded by a 'commit' command.",
		metaTags = { CmdMeta.updatesDatabase }
	) 
public class NewContextCommand extends Command<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		cmdContext.newObjectContext();
		return new OkResult();
	}

}
