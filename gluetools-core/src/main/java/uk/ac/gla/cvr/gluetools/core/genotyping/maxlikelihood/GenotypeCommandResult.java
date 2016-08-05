package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class GenotypeCommandResult extends BaseTableResult<GenotypeResult> {

	public GenotypeCommandResult(List<GenotypeResult> genotypeResults) {
		super("genotypeCommandResult", genotypeResults,
				column("sequenceName", gResult -> gResult.getSequenceName()),
				column("typeAlignmentName", gResult -> gResult.getTypeAlignmentName()),
				column("summaryCode", gResult -> gResult.getSummaryCode().name()),
				column("closestMemberAlignmentName", gResult -> gResult.getPlacementResult().getClosestMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)),
				column("closestMemberSourceName", gResult -> gResult.getPlacementResult().getClosestMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)),
				column("closestMemberSequenceID", gResult -> gResult.getPlacementResult().getClosestMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH)),
				column("likeWeightRatio", gResult -> gResult.getPlacementResult().getLikeWeightRatio()),
				column("distanceToClosestmember", gResult -> gResult.getPlacementResult().getDistanceToClosestMember().doubleValue())
				);
	}

}
