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
package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class SequenceException extends GlueException {

	public enum Code implements GlueErrorCode {
		SEQUENCE_FORMAT_ERROR("errorText"),
		SEQUENCE_FIELD_ERROR("errorText"),
		UNKNOWN_SEQUENCE_FORMAT("unknownFormat"),
		NO_DATA_PROVIDED(),
		BASE_64_FORMAT_EXCEPTION("errorText"),
		XML_SEQUENCE_DOES_NOT_CONTAIN_NUCLEOTIDES("primaryAccession"),
		UNABLE_TO_DETERMINE_SEQUENCE_FORMAT_FROM_BYTES, 
		UNABLE_TO_DETERMINE_SEQUENCE_FORMAT_FROM_FILE_EXTENSION("fileExtension");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public SequenceException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public SequenceException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
