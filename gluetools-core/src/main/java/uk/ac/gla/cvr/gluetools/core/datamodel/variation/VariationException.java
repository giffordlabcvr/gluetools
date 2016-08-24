package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class VariationException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		VARIATION_CODON_LOCATION_CAN_NOT_BE_USED_FOR_NUCLEOTIDE_VARIATIONS("refSeqName", "featureName", 
				"variationName"),
		VARIATION_LOCATION_OUT_OF_RANGE("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd"),
		VARIATION_ENDPOINTS_REVERSED("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd"), 
		AMINO_ACID_VARIATION_MUST_BE_DEFINED_ON_CODING_FEATURE("refSeqName", "featureName", 
				"variationName"),
		AMINO_ACID_VARIATION_HAS_NO_CODON_NUMBERING_ANCESTOR("refSeqName", "featureName", 
				"variationName"), 
		AMINO_ACID_VARIATION_NOT_CODON_ALIGNED("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd"),
		AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE("refSeqName", "featureName", 
				"variationName", "codonLabel", "firstLabeledCodon", "lastLabeledCodon"), 
		VARIATION_LOCATION_UNDEFINED("refSeqName", "featureName", "variationName"), 
		VARIATION_PATTERN_UNDEFINED("refSeqName", "featureName", "variationName"),
		WRONG_SCANNER_TYPE("refSeqName", "featureName", "variationName", "expectedSuperclass"),
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
