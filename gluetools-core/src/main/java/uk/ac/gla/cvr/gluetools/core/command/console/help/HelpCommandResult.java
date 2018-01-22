/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.text.WordUtils;

import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.InteractiveCommandResultRenderingContext;

public class HelpCommandResult extends ConsoleCommandResult {

	private Map<CommandGroup, List<SpecificCommandHelpLine>> cmdGroupToHelpLines;
	
	public HelpCommandResult(List<SpecificCommandHelpLine> helpLines) {
		super("");
		this.cmdGroupToHelpLines = helpLines.stream()
				.collect(Collectors.groupingBy(hl -> hl.getCmdGroup()));
	}

	
	
	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		List<CommandGroup> cmdGroups = new ArrayList<CommandGroup>(cmdGroupToHelpLines.keySet());
		Collections.sort(cmdGroups);
		
		cmdGroups.forEach( cmdGroup -> {
			List<SpecificCommandHelpLine> helpLines = cmdGroupToHelpLines.get(cmdGroup);
			Collections.sort(helpLines);
			buf.append(cmdGroup.getDescription()+":\n");
			int maxCommandWordsWidth = helpLines.stream().mapToInt(h -> h.joinedCommandWords().length()).max().orElse(0);
			int descriptionWidth = renderCtx.getTerminalWidth() - (maxCommandWordsWidth+5);
			final String formatterString0 = 
					"  %-"+Integer.toString(maxCommandWordsWidth)+
					"s - %-"+Integer.toString(descriptionWidth)+"s\n";
			final String formatterString = 
					"  %-"+Integer.toString(maxCommandWordsWidth)+
					"s   %-"+Integer.toString(descriptionWidth)+"s\n";
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
			buf.append("\n");
		});
		buf.append("For more detailed help, use: help <commandWord>...\n");
		renderCtx.output(buf.toString());

		
	}

	
	

}
