package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;

@CommandClass(
		commandWords={"commit"},
		docoptUsages={""},
		docoptOptions={},
		description="Commit any uncommitted changes to the database",
		furtherHelp="Most commands which update the database will commit these changes immediately. "+
		"However, some have an option to suppress the commit, via a -C or --noCommit option. This command can "+
		"be used subsequently to flush these changes to the database. "+
		"The main use of this is for efficiency: a large number of non-committed changes can be batched up and committed "+
		"together.",
		metaTags = { CmdMeta.updatesDatabase }
	) 
public class CommitCommand extends Command<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		cmdContext.commit();
		return new OkResult();
	}

}
