package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.Formatter;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;

public class HelpCommandResult extends ConsoleCommandResult {

	private List<HelpLine> helpLines;

	public HelpCommandResult(List<HelpLine> helpLines) {
		super();
		this.helpLines = helpLines;
	}

	@Override
	public void renderToConsole(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		try(Formatter formatter = new Formatter(buf)) {
			helpLines.stream().forEach(h -> {
				String commandWords = String.join(" ", h.getCommandWords());
				if(h instanceof GroupHelpLine) { commandWords += " ..."; }
				formatter.format("  %-22s - %-53s\n", commandWords, h.getDescription());
			});
		}
		buf.append("\nFor more detailed help, use: help <commandWord>...\n");
		renderCtx.output(buf.toString());
	}
	
	

}
