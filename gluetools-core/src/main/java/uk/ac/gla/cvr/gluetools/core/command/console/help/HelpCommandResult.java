package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.Formatter;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;

public class HelpCommandResult extends ConsoleCommandResult {


	public HelpCommandResult(List<HelpLine> helpLines) {
		super(renderHelpLines(helpLines));
	}

	private static String renderHelpLines(List<HelpLine> helpLines) {
		StringBuffer buf = new StringBuffer();
		try(Formatter formatter = new Formatter(buf)) {
			helpLines.stream().forEach(h -> {
				String commandWords = String.join(" ", h.getCommandWords());
				if(h instanceof GroupHelpLine) { commandWords += " ..."; }
				formatter.format("  %-23s - %-52s\n", commandWords, h.getDescription());
			});
		}
		buf.append("\nFor more detailed help, use: help <commandWord>...\n");
		return buf.toString();
	}
	
	

}
