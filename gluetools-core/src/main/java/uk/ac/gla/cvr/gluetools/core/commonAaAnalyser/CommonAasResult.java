package uk.ac.gla.cvr.gluetools.core.commonAaAnalyser;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class CommonAasResult extends BaseTableResult<CommonAminoAcids> {

	public static final String REFERENCE_NAME = "refName";
	public static final String FEATURE_NAME = "featureName";
	public static final String CODON_LABEL = "codon";
	public static final String COMMON_AAS = "commonAAs";
	
	public CommonAasResult(List<CommonAminoAcids> rowData) {
		super("commonAas", rowData,
				column(REFERENCE_NAME, aap -> aap.getRefName()),
				column(FEATURE_NAME, aap -> aap.getFeatureName()),
				column(CODON_LABEL, aap -> aap.getCodonLabel()),
				column(COMMON_AAS, aap -> String.join("/", aap.getCommonAas())));
	}

}
