package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;

import uk.ac.gla.cvr.gluetools.core.command.console.QuitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.SetDirectoryCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ShowDirectoryCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.help.HelpCommand;

public abstract class BaseCommandFactory extends CommandFactory {

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(QuitCommand.class);
		registerCommandClass(HelpCommand.class);
		addGroupHelp(Arrays.asList("console"), "Manage console settings");
		registerCommandClass(SetDirectoryCommand.class);
		registerCommandClass(ShowDirectoryCommand.class);
	}

}
