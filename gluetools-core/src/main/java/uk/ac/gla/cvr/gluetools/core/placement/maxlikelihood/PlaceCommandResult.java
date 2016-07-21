package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class PlaceCommandResult extends BaseTableResult<PlacementResult> {

	public PlaceCommandResult(List<PlacementResult> placementResults) {
		super("placementCommandResult", placementResults,
				column("sequence", gtr -> gtr.getSequenceName()),
				column("groupingAlignmentName", gtr -> gtr.getGroupingAlignmentName()),
				column("closestMemberAlmtName", gtr -> gtr.getClosestMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)),
				column("closestMemberSourceName", gtr -> gtr.getClosestMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)),
				column("closestMemberSequenceID", gtr -> gtr.getClosestMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH)),
				column("distanceToClosestMember", gtr -> gtr.getDistanceToClosestMember().doubleValue()),
				column("likeWeightRatio", gtr -> gtr.getLikeWeightRatio()));
	}
	
}
