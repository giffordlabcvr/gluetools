package uk.ac.gla.cvr.gluetools.core.command.console.help;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;

@SuppressWarnings("rawtypes")
public class HelpSpecificCommandResult extends ConsoleCommandResult {

	public HelpSpecificCommandResult(Class<? extends Command> cmdClass) {
		super(renderCmdHelp(cmdClass));
	}

	private static String renderCmdHelp(Class<? extends Command> cmdClass) {
		String cmdDesc = CommandUsage.descriptionForCmdClass(cmdClass);
		String command = String.join(" ", CommandUsage.cmdWordsForCmdClass(cmdClass));
		String usageString;
		if(CommandUsage.hasMetaTagForCmdClass(cmdClass, CmdMeta.inputIsComplex)) {
			usageString = "\nThis command has a complex input schema and may only be executed programmatically.\n";
		} else {
			usageString = CommandUsage.docoptStringForCmdClass(cmdClass, false);
		}
		String furtherHelp = CommandUsage.furtherHelpForCmdClass(cmdClass);
		String help = command +": "+cmdDesc+"\n"+usageString.trim();
		if(furtherHelp.length() > 0) {
			help = help+"\n"+furtherHelp;
		}
		return help;
	}

}
