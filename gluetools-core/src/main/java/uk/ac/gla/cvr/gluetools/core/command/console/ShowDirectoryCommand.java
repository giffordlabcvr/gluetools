package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="show-directory")
@CommandClass(
		description = "Show the current directory for loading and saving",
		docoptUsages = {""})
public class ShowDirectoryCommand extends ConsoleCommand {

	@Override
	protected CommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		final String path = cmdContext.getLoadSavePath().getAbsolutePath();
		return new ConsoleCommandResult() {
			@Override
			public String getResultAsConsoleText() {
				return path;
			}
		};
	}

}