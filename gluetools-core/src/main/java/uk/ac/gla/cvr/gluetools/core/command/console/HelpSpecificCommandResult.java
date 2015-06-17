package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;

public class HelpSpecificCommandResult extends ConsoleCommandResult {

	private Class<? extends Command> cmdClass;
	
	public HelpSpecificCommandResult(Class<? extends Command> cmdClass) {
		super();
		this.cmdClass = cmdClass;
	}

	@Override
	public String getResultAsConsoleText() {
		String cmdDesc = CommandUsage.descriptionForCmdClass(cmdClass);
		String command = CommandUsage.commandForCmdClass(cmdClass);
		String docoptString = CommandUsage.docoptStringForCmdClass(cmdClass);
		return command +": "+cmdDesc+"\n"+docoptString.trim();
	}

}
