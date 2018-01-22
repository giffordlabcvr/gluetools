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
package uk.ac.gla.cvr.gluetools.core.datamodel.alignment;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class AlignmentException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		PARENT_RELATIONSHIP_LOOP("alignmentNames"),
		REFERENCE_NOT_MEMBER_OF_PARENT("alignmentName", "parentAlignmentName", "referenceName"),
		REFERENCE_DOES_NOT_CONSTRAIN_ANCESTOR("referenceName", "alignmentName"),
		ALIGNMENT_NOT_CHILD_OF_PARENT("alignmentName", "parentAlignmentName"),
		ALIGNMENT_IS_UNCONSTRAINED("alignmentName"),
		CANNOT_SPECIFY_RECURSIVE_FOR_UNCONSTRAINED_ALIGNMENT("alignmentName"); 

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public AlignmentException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public AlignmentException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
