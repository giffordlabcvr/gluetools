package uk.ac.gla.cvr.gluetools.core.command.console.help;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;

public class HelpSpecificCommandResult extends ConsoleCommandResult {

	private Class<? extends Command> cmdClass;
	
	public HelpSpecificCommandResult(Class<? extends Command> cmdClass) {
		super();
		this.cmdClass = cmdClass;
	}

	@Override
	public String getResultAsConsoleText() {
		String cmdDesc = CommandUsage.descriptionForCmdClass(cmdClass);
		String command = String.join(" ", CommandUsage.cmdWordsForCmdClass(cmdClass));
		String docoptString = CommandUsage.docoptStringForCmdClass(cmdClass, null, false);
		String furtherHelp = CommandUsage.furtherHelpForCmdClass(cmdClass);
		String help = command +": "+cmdDesc+"\n"+docoptString.trim();
		if(furtherHelp.length() > 0) {
			help = help+"\n"+furtherHelp;
		}
		return help;
	}

}
