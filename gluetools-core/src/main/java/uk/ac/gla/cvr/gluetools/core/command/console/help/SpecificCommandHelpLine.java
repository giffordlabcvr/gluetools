package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;

@SuppressWarnings("rawtypes")
public class SpecificCommandHelpLine extends HelpLine {

	private Class<? extends Command> cmdClass;

	public SpecificCommandHelpLine(Class<? extends Command> cmdClass) {
		super();
		this.cmdClass = cmdClass;
	}

	public Class<? extends Command> getCmdClass() {
		return cmdClass;
	}

	@Override
	public List<String> getCommandWords() {
		return Arrays.asList(CommandUsage.cmdWordsForCmdClass(getCmdClass()));
	}

	@Override
	public String getDescription() {
		return CommandUsage.descriptionForCmdClass(getCmdClass());
	}
	

}
