package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.reporting.VariationScanMemberCount;

public class AlignmentVariationFrequencyResult extends BaseTableResult<VariationScanMemberCount> {

	public static final String 
		REF_SEQ_NAME = "referenceName",
		FEATURE_NAME = "featureName",
		VARIATION_NAME = "variationName",
		MEMBERS_PRESENT = "membersPresent",
		PCT_PRESENT = "pctPresent",
		MEMBERS_ABSENT = "membersAbsent",
		PCT_ABSENT = "pctAbsent";


	public AlignmentVariationFrequencyResult(List<VariationScanMemberCount> rowData) {
		super("samVariationsScanResult", 
				rowData,
				column(REF_SEQ_NAME, vsmc -> vsmc.getVariation().getFeatureLoc().getReferenceSequence().getName()), 
				column(FEATURE_NAME, vsmc -> vsmc.getVariation().getFeatureLoc().getFeature().getName()), 
				column(VARIATION_NAME, vsmc -> vsmc.getVariation().getName()), 
				column(MEMBERS_PRESENT, vsmc -> vsmc.getMembersWherePresent()), 
				column(PCT_PRESENT, vsmc -> vsmc.getPctWherePresent()), 
				column(MEMBERS_ABSENT, vsmc -> vsmc.getMembersWhereAbsent()),
				column(PCT_ABSENT, vsmc -> vsmc.getPctWhereAbsent()));
	}

}
