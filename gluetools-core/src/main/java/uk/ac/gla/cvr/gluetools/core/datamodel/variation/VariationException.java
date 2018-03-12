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
package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class VariationException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		VARIATION_CODON_LOCATION_CAN_NOT_BE_USED_FOR_NUCLEOTIDE_VARIATIONS("refSeqName", "featureName", 
				"variationName"),
		VARIATION_LOCATION_OUT_OF_RANGE("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd"),
		VARIATION_LOCATION_ENDPOINTS_REVERSED("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd"), 
		AMINO_ACID_VARIATION_MUST_BE_DEFINED_ON_CODING_FEATURE("refSeqName", "featureName", 
				"variationName"),
		AMINO_ACID_VARIATION_HAS_NO_CODON_NUMBERING_ANCESTOR("refSeqName", "featureName", 
				"variationName"), 
		AMINO_ACID_VARIATION_NOT_CODON_ALIGNED("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd"),
		AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE("refSeqName", "featureName", 
				"variationName", "codonLabel", "firstLabeledCodon", "lastLabeledCodon"), 
		VARIATION_SCANNER_EXCEPTION("refSeqName", "featureName", "variationName", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public VariationException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public VariationException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
