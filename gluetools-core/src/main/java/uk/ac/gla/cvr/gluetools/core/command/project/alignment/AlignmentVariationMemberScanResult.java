package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class AlignmentVariationMemberScanResult extends BaseTableResult<MemberVariationScanResult> {


	public static final String 
		ALIGNMENT_NAME = "alignmentName",
		SOURCE_NAME = "sourceName",
		SEQUENCE_ID = "sequenceID",
		PRESENT = "present";


	public AlignmentVariationMemberScanResult(List<MemberVariationScanResult> rowData) {
		super("alignmentVariationMemberScanResult", rowData,
				column(ALIGNMENT_NAME, mvsr -> mvsr.getAlignmentMember().getAlignment().getName()),
				column(SOURCE_NAME, mvsr -> mvsr.getAlignmentMember().getSequence().getSource().getName()),
				column(SEQUENCE_ID, mvsr -> mvsr.getAlignmentMember().getSequence().getSequenceID()),
				column(PRESENT, mvsr -> mvsr.getVariationScanResult().isPresent()));
	}


}
