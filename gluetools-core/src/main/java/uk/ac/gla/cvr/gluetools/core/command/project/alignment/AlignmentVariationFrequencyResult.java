package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.reporting.VariationScanMemberCount;

public class AlignmentVariationFrequencyResult extends BaseTableResult<VariationScanMemberCount> {

	public static final String 
		VARIATION_NAME = "variationName",
		READS_PRESENT = "membersPresent",
		PCT_PRESENT = "pctPresent",
		READS_ABSENT = "membersAbsent",
		PCT_ABSENT = "pctAbsent";


	public AlignmentVariationFrequencyResult(List<VariationScanMemberCount> rowData) {
		super("samVariationsScanResult", 
				rowData,
				column(VARIATION_NAME, vsmc -> vsmc.getVariationName()), 
				column(READS_PRESENT, vsmc -> vsmc.getMembersWherePresent()), 
				column(PCT_PRESENT, vsmc -> vsmc.getPctWherePresent()), 
				column(READS_ABSENT, vsmc -> vsmc.getMembersWhereAbsent()),
				column(PCT_ABSENT, vsmc -> vsmc.getPctWhereAbsent()));
	}

}