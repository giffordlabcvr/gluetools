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
package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@SuppressWarnings("rawtypes")
@CommandClass(
	commandWords={"help"},
	docoptUsages={"[<commandWord> ...]"},
	metaTags = { CmdMeta.consoleOnly },
	description="Command help, based on a word sequence"
) 
public class HelpCommand extends Command<ConsoleCommandResult> {

	private List<String> commandWords;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		commandWords = PluginUtils.configureStringsProperty(configElem, "commandWord");
	}

	
	@Override
	public ConsoleCommandResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		CommandFactory commandFactory = cmdContext.peekCommandMode().getCommandFactory();
		List<SpecificCommandHelpLine> helpLines = commandFactory.helpLinesForCommandWords(consoleCommandContext, commandWords);
		if(helpLines.isEmpty()) {
			throw new CommandException(CommandException.Code.UNKNOWN_COMMAND, String.join(" ", commandWords), cmdContext.getModePath());
		} else if(helpLines.size() == 1) {
			return new HelpSpecificCommandResult(cmdContext, helpLines.get(0).getCmdClass());
		} else {
			return new HelpCommandResult(helpLines);
		}
	}

	@CompleterClass
	public static class HelpCommandCompleter extends CommandCompleter {
		@Override
		public List<CompletionSuggestion> completionSuggestions(ConsoleCommandContext commandContext, Class<? extends Command> cmdClass, List<String> argStrings, String prefix, boolean includeOptions) {
			CommandFactory commandFactory = commandContext.peekCommandMode().getCommandFactory();
			return commandFactory.getCommandWordSuggestions(commandContext, argStrings, prefix, false, commandContext.isRequireModeWrappable(), true);
		}
		
	}

	
}
