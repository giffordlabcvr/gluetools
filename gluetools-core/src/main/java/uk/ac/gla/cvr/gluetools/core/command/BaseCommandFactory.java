/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.config.ConfigShowPropertyCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.CommitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.NewContextCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.QuitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.RootModeCommand;
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
import uk.ac.gla.cvr.gluetools.core.command.fileUtils.FileUtilLoadStringCommand;
import uk.ac.gla.cvr.gluetools.core.command.fileUtils.FileUtilSaveStringCommand;

public abstract class BaseCommandFactory extends CommandFactory {

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		setCmdGroup(new CommandGroup("otherNonModeSpecific", "Other commands (non-mode specific)", 100, true));
		registerCommandClass(HelpCommand.class);
		registerCommandClass(QuitCommand.class);
		
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(RootModeCommand.class);
		
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
		registerCommandClass(FileUtilLoadStringCommand.class);

		setCmdGroup(new CommandGroup("console", "Commands to manage console options", 95, true));
		registerCommandClass(ConsoleChangeDirectoryCommand.class);
		registerCommandClass(ConsoleShowOptionCommand.class);
		registerCommandClass(ConsoleSetOptionCommand.class);
		registerCommandClass(ConsoleUnsetOptionCommand.class);
		registerCommandClass(ConsoleHelpOptionCommand.class);
		registerCommandClass(ConsoleAddOptionLineCommand.class);
		registerCommandClass(ConsoleRemoveOptionLineCommand.class);

		setCmdGroup(new CommandGroup("config", "Commands to manage GLUE engine version/configuration", 96, true));
		registerCommandClass(ConfigShowPropertyCommand.class);
		registerCommandClass(GlueEngineShowVersionCommand.class);
		
		setCmdGroup(null);
	}

}
