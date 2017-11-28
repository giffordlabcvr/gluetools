package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.config.ConfigShowPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.CommitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.NewContextCommand;
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
import uk.ac.gla.cvr.gluetools.core.command.fileUtils.FileUtilSaveStringCommand;

public abstract class BaseCommandFactory extends CommandFactory {

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		setCmdGroup(new CommandGroup("otherNonModeSpecific", "Other commands", 100, true));
		registerCommandClass(HelpCommand.class);
		registerCommandClass(QuitCommand.class);
		setCmdGroup(new CommandGroup("scripting", "Commands for accessing the scripting layer", 92, true));
		registerCommandClass(RunFileCommand.class);
		registerCommandClass(RunScriptCommand.class);
		setCmdGroup(new CommandGroup("database", "Commands for optimising database access", 93, true));
		registerCommandClass(CommitCommand.class);
		registerCommandClass(NewContextCommand.class);

		setCmdGroup(new CommandGroup("file-utils", "File system utility commands", 94, true));
		registerCommandClass(FileUtilListFilesCommand.class);
		registerCommandClass(FileUtilDeleteFileCommand.class);
		registerCommandClass(FileUtilSaveStringCommand.class);

		setCmdGroup(new CommandGroup("console", "Commands to manage console options", 95, true));
		registerCommandClass(ConsoleChangeDirectoryCommand.class);
		registerCommandClass(ConsoleShowOptionCommand.class);
		registerCommandClass(ConsoleSetOptionCommand.class);
		registerCommandClass(ConsoleUnsetOptionCommand.class);
		registerCommandClass(ConsoleHelpOptionCommand.class);
		registerCommandClass(ConsoleAddOptionLineCommand.class);
		registerCommandClass(ConsoleRemoveOptionLineCommand.class);

		setCmdGroup(new CommandGroup("config", "Commands to manage GLUE engine configuration", 96, true));
		registerCommandClass(ConfigShowPropertyCommand.class);
		
		setCmdGroup(null);
	}

}
