package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.List;

public abstract class HelpLine implements Comparable<HelpLine> {

	public abstract List<String> getCommandWords();

	public abstract String getDescription();
	
	public String joinedCommandWords() {
		return String.join(" ", getCommandWords());
	}

	@Override
	public int compareTo(HelpLine o) {
		String thisWords = joinedCommandWords();
		String otherWords = o.joinedCommandWords();
		return thisWords.compareTo(otherWords);
	}
	

}
