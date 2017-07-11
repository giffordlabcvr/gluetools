package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;

import uk.ac.gla.cvr.gluetools.core.command.config.ConfigShowPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.CommitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.QuitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.RunFileCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.RunScriptCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleAddOptionLineCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleChangeDirectoryCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleHelpOptionCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleRemoveOptionLineCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleSetOptionCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleShowOptionCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleUnsetOptionCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.help.HelpCommand;
import uk.ac.gla.cvr.gluetools.core.command.fileUtils.FileUtilDeleteFileCommand;
import uk.ac.gla.cvr.gluetools.core.command.fileUtils.FileUtilListFilesCommand;

public abstract class BaseCommandFactory extends CommandFactory {

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(QuitCommand.class);
		registerCommandClass(HelpCommand.class);
		registerCommandClass(RunFileCommand.class);
		registerCommandClass(RunScriptCommand.class);
		registerCommandClass(CommitCommand.class);

		registerCommandClass(FileUtilListFilesCommand.class);
		registerCommandClass(FileUtilDeleteFileCommand.class);

		addGroupHelp(Arrays.asList("console"), "Manage console options");
		registerCommandClass(ConsoleChangeDirectoryCommand.class);
		registerCommandClass(ConsoleShowOptionCommand.class);
		registerCommandClass(ConsoleSetOptionCommand.class);
		registerCommandClass(ConsoleUnsetOptionCommand.class);
		registerCommandClass(ConsoleHelpOptionCommand.class);

		registerCommandClass(ConsoleAddOptionLineCommand.class);
		registerCommandClass(ConsoleRemoveOptionLineCommand.class);

		addGroupHelp(Arrays.asList("config"), "Manage engine configuration");
		registerCommandClass(ConfigShowPropertyCommand.class);
	}

}
