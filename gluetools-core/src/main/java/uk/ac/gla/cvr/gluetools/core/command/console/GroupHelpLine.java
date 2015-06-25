package uk.ac.gla.cvr.gluetools.core.command.console;

import java.util.List;

public class GroupHelpLine extends HelpLine {

	private List<String> commandWords;
	private String description;
	
	public GroupHelpLine(List<String> commandWords, String description) {
		this.commandWords = commandWords;
		this.description = description;
	}

	@Override
	public List<String> getCommandWords() {
		return commandWords;
	}

	@Override
	public String getDescription() {
		return description;
	}
	

}
