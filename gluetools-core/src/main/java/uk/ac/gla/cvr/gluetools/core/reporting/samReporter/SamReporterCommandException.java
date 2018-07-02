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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class SamReporterCommandException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		NO_TARGET_REFERENCE_DEFINED(),
		NO_PLACEMENT_NEIGHBOURS_FOUND("cutoffDistance"),
		NO_CONSENSUS_PLACEMENTS(),
		TARGET_REFERENCE_NOT_FOUND("samReferenceName"),
		TARGET_REFERENCE_AMBIGUOUS("samReferenceName", "targetRefNames"),
		TIP_ALIGNMENT_NOT_FOUND("samReferenceName"),
		TIP_ALIGNMENT_AMBIGUOUS("samReferenceName", "tipAlmtNames"),
		NO_SAM_CONSENSUS("minQScore", "minMapQ", "minDepth"),
		ILLEGAL_SAM_REF_SENSE("illegalSamRefSense", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected SamReporterCommandException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public SamReporterCommandException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
}
