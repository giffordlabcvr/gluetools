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
package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FastaAlignmentImporterException extends GlueException {
	
	public enum Code implements GlueErrorCode {
		ALIGNMENT_IS_CONSTRAINED("alignmentName", "referenceName"),
		NO_FASTA_ID_REGEX_MATCH("fastaId"),
		INVALID_WHERE_CLAUSE("fastaId", "whereClause"),
		NO_SEQUENCE_FOUND("fastaId", "whereClause"),
		MULTIPLE_SEQUENCES_FOUND("fastaId", "whereClause"),
		SUBSEQUENCE_NOT_FOUND("startColumnNumber", "endColumnNumber", "fastaId", "whereClause"), 
		AMBIGUOUS_SEGMENT("startColumnNumber", "endColumnNumber", "fastaId", "whereClause", "fromPosition"), 
		MISSING_COVERAGE("startColumnNumber", "endColumnNumber", "fastaId", "whereClause"), 
		SEGMENT_OVERLAPS_EXISTING("startColumnNumber", "endColumnNumber", "fastaId", "whereClause"), 
		NAVIGATION_ALIGNMENT_REQUIRED(),
		NAVIGATION_ALIGNMENT_IS_UNCONSTRAINED("navAlignmentName"),
		NAVIGATION_ALIGNMENT_MEMBER_NOT_FOUND("navAlignmentName", "memberSourceName", "memberSequenceID"),
		NAVIGATION_REF_SEQ_FEATURE_MISSING("navRefSeqName", "navAlignmentName", "featureName"),
		NAVIGATION_REF_SEQ_FEATURE_HAS_NO_SEGMENTS("navRefSeqName", "navAlignmentName", "featureName"),
		NAVIGATION_ALIGNMENT_MEMBER_DOES_NOT_COVER_FEATURE("navAlignmentName", "memberSourceName", "memberSequenceID", "featureName");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public FastaAlignmentImporterException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FastaAlignmentImporterException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
