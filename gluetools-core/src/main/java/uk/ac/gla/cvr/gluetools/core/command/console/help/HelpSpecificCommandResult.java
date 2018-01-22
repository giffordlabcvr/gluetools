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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.text.WordUtils;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;

@SuppressWarnings("rawtypes")
public class HelpSpecificCommandResult extends ConsoleCommandResult {

	public HelpSpecificCommandResult(CommandContext cmdContext, Class<? extends Command> cmdClass) {
		super(renderCmdHelp(cmdContext, cmdClass), false);
	}

	private static String renderCmdHelp(CommandContext cmdContext, Class<? extends Command> cmdClass) {
		String cmdDesc = CommandUsage.descriptionForCmdClass(cmdClass);
		String command = String.join(" ", CommandUsage.cmdWordsForCmdClass(cmdClass));
		String usageString;
		if(CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.inputIsComplex)) {
			usageString = "\nThis command has a complex input schema and may only be executed programmatically.\n";
		} else {
			usageString = CommandUsage.docoptStringForCmdClass(cmdClass, false);
		}
		String help = command +": "+cmdDesc+"\n"+usageString.trim();

		String furtherHelp = CommandUsage.furtherHelpForCmdClass(cmdClass);
		if(cmdContext instanceof ConsoleCommandContext) {
			int terminalWidth = ((ConsoleCommandContext) cmdContext).getTerminalWidth();
			furtherHelp =
					String.join("\n", 
							Arrays.asList(furtherHelp.split("\\n")).stream()
								.map(line -> WordUtils.wrap(line, terminalWidth, "\n", false))
								.collect(Collectors.toList())
								);
		}
		if(furtherHelp.length() > 0) {
			help = help+"\n"+furtherHelp;
		}
		return help;
	}

}
