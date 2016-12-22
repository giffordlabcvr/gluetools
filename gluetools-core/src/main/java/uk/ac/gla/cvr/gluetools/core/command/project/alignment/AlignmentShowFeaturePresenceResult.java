package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class AlignmentShowFeaturePresenceResult extends BaseTableResult<FeaturePresence> {

	public static final String 
	FEATURE_NAME = "featureName",
	MEMBERS_WHERE_PRESENT = "membersWherePresent",
	TOTAL_MEMBERS = "totalMembers",
	PERCENT_WHERE_PRESENT = "percentWherePresent";


	public AlignmentShowFeaturePresenceResult(List<FeaturePresence> rowData) {
		super("alignmentShowFeaturePresenceResult", rowData,
				column(FEATURE_NAME, fp -> fp.getFeatureLoc().getFeature().getName()),
				column(MEMBERS_WHERE_PRESENT, fp -> fp.getMembersWherePresent()),
				column(TOTAL_MEMBERS, fp -> fp.getTotalMembers()),
				column(PERCENT_WHERE_PRESENT, fp -> fp.getPercentWherePresent()));
	}


}
