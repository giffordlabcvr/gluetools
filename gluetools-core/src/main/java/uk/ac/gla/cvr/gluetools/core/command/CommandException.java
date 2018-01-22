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

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class CommandException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		UNKNOWN_COMMAND("unknownCommandText", "commandModePath"), 
		NOT_A_MODE_COMMAND("commandText", "commandModePath"), 
		COMMAND_USAGE_ERROR("errorText"), 
		COMMAND_FAILED_ERROR("errorText"), 
		COMMAND_DOES_NOT_CONSUME_BINARY("commandWords"), 
		NOT_EXECUTABLE_IN_CONTEXT("commandWords", "contextDescription"), 
		ARGUMENT_FORMAT_ERROR("argName", "errorText", "argValue"), 
		COMMAND_BINARY_INPUT_IO_ERROR("commandWords", "errorText"), 
		UNKNOWN_MODE_PATH("commandModePath"),
		COMMAND_RESULT_NOT_A_TABLE("cmdClass", "resultClass"),
		INVALID_RESULT_FORMAT("invalidFormat"),
		INVALID_LINE_FEED_STYLE("invalidLineFeedStyle");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public CommandException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public CommandException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
