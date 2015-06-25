package uk.ac.gla.cvr.gluetools.core.command.console;

import java.util.Formatter;
import java.util.List;

public class HelpCommandResult extends ConsoleCommandResult {

	private List<HelpLine> helpLines;

	public HelpCommandResult(List<HelpLine> helpLines) {
		super();
		this.helpLines = helpLines;
	}

	@Override
	public String getResultAsConsoleText() {
		StringBuffer buf = new StringBuffer();
		try(Formatter formatter = new Formatter(buf)) {
			helpLines.stream().forEach(h -> {
				String commandWords = String.join(" ", h.getCommandWords());
				if(h instanceof GroupHelpLine) { commandWords += " ..."; }
				formatter.format("  %-20s - %-55s\n", commandWords, h.getDescription());
			});
		}
		buf.append("\nFor more detailed help, use: help <commandWord>...\n");
		return buf.toString();
	}
	
	

}
