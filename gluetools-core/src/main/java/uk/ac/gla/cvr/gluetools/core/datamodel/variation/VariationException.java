package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class VariationException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		VARIATION_LOCATION_OUT_OF_RANGE("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd"),
		VARIATION_ENDPOINTS_REVERSED("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd"), 
		AMINO_ACID_VARIATION_MUST_BE_DEFINED_IN_ORF("refSeqName", "featureName", 
				"variationName"),
		AMINO_ACID_VARIATION_HAS_NO_CODON_NUMBERING_ANCESTOR("refSeqName", "featureName", 
				"variationName"), 
		AMINO_ACID_VARIATION_LOCATION_OUT_OF_RANGE("refSeqName", "featureName", 
				"variationName", "codonAncestorFeatureName", "refStart", "refEnd", "maxCodonNumber")
		;

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
