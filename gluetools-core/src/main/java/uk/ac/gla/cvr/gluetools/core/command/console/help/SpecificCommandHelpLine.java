package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;

@SuppressWarnings("rawtypes")
public class SpecificCommandHelpLine implements Comparable<SpecificCommandHelpLine> {

	private Class<? extends Command> cmdClass;
	private CommandGroup cmdGroup;

	public SpecificCommandHelpLine(Class<? extends Command> cmdClass, CommandGroup cmdGroup) {
		super();
		this.cmdClass = cmdClass;
		this.cmdGroup = cmdGroup;
	}

	public Class<? extends Command> getCmdClass() {
		return cmdClass;
	}

	public List<String> getCommandWords() {
		return Arrays.asList(CommandUsage.cmdWordsForCmdClass(getCmdClass()));
	}

	public String joinedCommandWords() {
		return String.join(" ", getCommandWords());
	}
	
	public String getDescription() {
		return CommandUsage.descriptionForCmdClass(getCmdClass());
	}

	public CommandGroup getCmdGroup() {
		return cmdGroup;
	}
	
	@Override
	public int compareTo(SpecificCommandHelpLine o) {
		String thisWords = joinedCommandWords();
		String otherWords = o.joinedCommandWords();
		return thisWords.compareTo(otherWords);
	}


}
