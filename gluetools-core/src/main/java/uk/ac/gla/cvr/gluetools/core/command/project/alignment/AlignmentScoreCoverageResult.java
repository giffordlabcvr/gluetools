package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class AlignmentScoreCoverageResult extends BaseTableResult<AlignmentCoverageScore> {

	public static final String SCORE = "score";
	
	public AlignmentScoreCoverageResult(List<AlignmentCoverageScore> rowData) {
		super("alignmentScoreCoverageResult", rowData,
				column(AlignmentMember.SOURCE_NAME_PATH, acs -> acs.getAlmtMember().getSequence().getSource().getName()),
				column(AlignmentMember.SEQUENCE_ID_PATH, acs -> acs.getAlmtMember().getSequence().getSequenceID()),
				column(SCORE, acs -> acs.getScore()));
	}

}
