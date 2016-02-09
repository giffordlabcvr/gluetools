package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.Formatter;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;

public class HelpCommandResult extends ConsoleCommandResult {

	private List<HelpLine> helpLines;
	
	public HelpCommandResult(List<HelpLine> helpLines) {
		super("");
		this.helpLines = helpLines;
	}

	
	
	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		int maxCommandWordsWidth = helpLines.stream().mapToInt(h -> h.joinedCommandWords().length()).max().orElse(0);
		int descriptionWidth = renderCtx.getTerminalWidth() - (maxCommandWordsWidth+5);
		final String formatterString0 = 
				"  %-"+Integer.toString(maxCommandWordsWidth)+
				"s - %-"+Integer.toString(descriptionWidth)+"s\n";
		final String formatterString = 
				"  %-"+Integer.toString(maxCommandWordsWidth)+
				"s   %-"+Integer.toString(descriptionWidth)+"s\n";
		StringBuffer buf = new StringBuffer();
		try(Formatter formatter = new Formatter(buf)) {
			helpLines.stream().forEach(h -> {
				String commandWords = h.joinedCommandWords();
				String[] wrappedLines = WordUtils.wrap(h.getDescription(), descriptionWidth, "\n", false).split("\\n");
				formatter.format(formatterString0, commandWords, wrappedLines[0]);
				for(int i = 1; i < wrappedLines.length; i++) {
					formatter.format(formatterString, "", wrappedLines[i]);
				}
			});
		}
		buf.append("\nFor more detailed help, use: help <commandWord>...\n");
		renderCtx.output(buf.toString());

		
	}

	
	

}
