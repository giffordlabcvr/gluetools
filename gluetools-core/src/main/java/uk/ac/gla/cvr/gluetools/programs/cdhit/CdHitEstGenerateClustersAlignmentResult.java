package uk.ac.gla.cvr.gluetools.programs.cdhit;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class CdHitEstGenerateClustersAlignmentResult extends BaseTableResult<CdHitEstGenerateClustersAlignmentResultRow>{

	public CdHitEstGenerateClustersAlignmentResult(List<CdHitEstGenerateClustersAlignmentResultRow> rowData) {
		super("cdHitEstGenerateClustersAlignmentResult", rowData,
				column("clusterNumber", chegcarr -> chegcarr.getClusterNumber()),
				column(AlignmentMember.ALIGNMENT_NAME_PATH, chegcarr -> chegcarr.getMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)),
				column(AlignmentMember.SOURCE_NAME_PATH, chegcarr -> chegcarr.getMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)),
				column(AlignmentMember.SEQUENCE_ID_PATH, chegcarr -> chegcarr.getMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH)),
				column("isRepresentative", chegcarr -> chegcarr.isRepresentative()));
	}
	
}
