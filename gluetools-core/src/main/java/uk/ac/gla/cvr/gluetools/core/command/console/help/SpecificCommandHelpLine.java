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
