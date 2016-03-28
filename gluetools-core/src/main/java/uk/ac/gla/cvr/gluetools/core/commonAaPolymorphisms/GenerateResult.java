package uk.ac.gla.cvr.gluetools.core.commonAaPolymorphisms;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class GenerateResult extends BaseTableResult<AaPolymorphism> {

	public static final String REFERENCE_NAME = "refName";
	public static final String FEATURE_NAME = "featureName";
	public static final String VARIATION_NAME = "variationName";
	public static final String CODON_LABEL = "codon";
	public static final String REFERENCE_AA = "refAA";
	public static final String VARIATION_AA = "variationAA";
	
	public GenerateResult(List<AaPolymorphism> rowData) {
		super("generateAaPolymorphismsResult", rowData,
				column(REFERENCE_NAME, aap -> aap.getRefName()),
				column(FEATURE_NAME, aap -> aap.getFeatureName()),
				column(VARIATION_NAME, aap -> aap.getVariationName()),
				column(CODON_LABEL, aap -> aap.getCodonLabel()),
				column(REFERENCE_AA, aap -> aap.getRefAa()),
				column(VARIATION_AA, aap -> aap.getVariationAa()));
	}

}
