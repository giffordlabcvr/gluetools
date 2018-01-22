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
package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;

public class CommandBuilder<R extends CommandResult, C extends Command<R>> {

	private CommandContext cmdContext;
	private CommandDocument documentBuilder;
	private Class<C> cmdClass;
	private CommandObject cmdObjectBuilder;
	
	public CommandBuilder(CommandContext cmdContext, Class<C> cmdClass) {
		super();
		this.cmdContext = cmdContext;
		this.cmdClass = cmdClass;
		cmdContext.checkCommmandIsExecutable(cmdClass);
		String[] cmdWords = CommandUsage.cmdWordsForCmdClass(cmdClass);
		documentBuilder = new CommandDocument(cmdWords[0]);
		cmdObjectBuilder = documentBuilder;
		for(int i = 1; i < cmdWords.length; i++) {
			cmdObjectBuilder = cmdObjectBuilder.setObject(cmdWords[i]);
		}
	}

	public CommandBuilder<R, C> setInt(String name, int value) {
		cmdObjectBuilder.setInt(name, value);
		return this;
	}

	public CommandBuilder<R, C> setBoolean(String name, boolean value) {
		cmdObjectBuilder.setBoolean(name, value);
		return this;
	}

	public CommandBuilder<R, C> setDouble(String name, double value) {
		cmdObjectBuilder.setDouble(name, value);
		return this;
	}

	public CommandBuilder<R, C> setNull(String name) {
		cmdObjectBuilder.setNull(name);
		return this;
	}

	public CommandBuilder<R, C> setString(String name, String value) {
		cmdObjectBuilder.setString(name, value);
		return this;
	}

	public CommandBuilder<R, C> set(String name, Object value) {
		cmdObjectBuilder.set(name, value);
		return this;
	}

	public CommandArray setArray(String name) {
		return cmdObjectBuilder.setArray(name);
	}
	
	public C build() {
		return cmdClass.cast(cmdContext.commandFromElement(CommandDocumentXmlUtils.commandDocumentToXmlDocument(documentBuilder).getDocumentElement()));
	}

	@SuppressWarnings("rawtypes")
	public R execute() {
		C command = build();
		Class<? extends Command> cmdClass = command.getClass();
		cmdContext.checkCommmandIsExecutable(cmdClass);
		return command.execute(cmdContext);
	}

	
	
}
