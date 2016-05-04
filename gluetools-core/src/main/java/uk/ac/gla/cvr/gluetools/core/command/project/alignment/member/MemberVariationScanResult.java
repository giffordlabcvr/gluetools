package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationScanResult;

public class MemberVariationScanResult extends BaseTableResult<VariationScanResult> {

	public static final String 
		REF_SEQ_NAME = "referenceName",
		FEATURE_NAME = "featureName",
		VARIATION_NAME = "variationName",
		PRESENT = "present",
		ABSENT = "absent";


	public MemberVariationScanResult(List<VariationScanResult> rowData) {
		super("memberVariationScanResult", 
				rowData, 
				column(REF_SEQ_NAME, vsr -> vsr.getVariation().getFeatureLoc().getReferenceSequence().getName()),
				column(FEATURE_NAME, vsr -> vsr.getVariation().getFeatureLoc().getFeature().getName()),
				column(VARIATION_NAME, vsr -> vsr.getVariation().getName()),
				column(PRESENT, vsr -> vsr.isPresent()),
				column(ABSENT, vsr -> vsr.isAbsent()));
	}

}
