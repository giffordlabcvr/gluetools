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
package uk.ac.gla.cvr.gluetools.core.console;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ConsoleException extends GlueException {

	public enum Code implements GlueErrorCode {
	
		SYNTAX_ERROR("errorPosition"),
		INVALID_PATH("invalidPath", "errorText"), 
		FILE_NOT_FOUND("path"), 
		NOT_A_FILE("path"), 
		NOT_A_DIRECTORY("path"), 
		FILE_NOT_READABLE("path"), 
		FILE_NOT_WRITEABLE("path"), 
		READ_ERROR("path", "errorTxt"),
		WRITE_ERROR("path", "errorTxt"),
		FILE_CREATION_ERROR("path", "errorTxt"),
		COMMAND_NOT_WRAPPABLE("commandWords", "commandModePath"), 
		COMMAND_HAS_COMPLEX_INPUT("commandWords"), 
		MAKE_DIRECTORY_ERROR("path");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public ConsoleException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ConsoleException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
