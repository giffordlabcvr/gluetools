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
package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ProjectModeCommandException extends GlueException {

	public enum Code implements GlueErrorCode {
		INVALID_PROPERTY("invalidProperty", "validProperties", "tableName"),
		INCOMPATIBLE_TYPES_FOR_COPY("fromFieldName", "fromType", "toFieldName", "toType"), 
		NO_SUCH_MODIFIABLE_PROPERTY("tableName", "fieldName"), 
		NO_SUCH_PROPERTY("tableName", "fieldName"),
		NO_SUCH_TABLE("tableName"),
		NO_SUCH_CUSTOM_TABLE("tableName"),
		INCORRECT_FIELD_TYPE("tableName", "fieldName", "requiredFieldType", "actualFieldType"),
		INVALID_TARGET_PATH("tableName", "targetPath", "correctForm");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public ProjectModeCommandException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ProjectModeCommandException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
