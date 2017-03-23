package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class AlignmentVariationMemberScanResult extends BaseTableResult<MemberVariationScanResult> {


	public static final String 
		ALIGNMENT_NAME = "alignmentName",
		SOURCE_NAME = "sourceName",
		SEQUENCE_ID = "sequenceID",
		PRESENT = "present";


	public AlignmentVariationMemberScanResult(List<MemberVariationScanResult> rowData) {
		super("alignmentVariationMemberScanResult", rowData,
				column(ALIGNMENT_NAME, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.ALIGNMENT_NAME_PATH)),
				column(SOURCE_NAME, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.SOURCE_NAME_PATH)),
				column(SEQUENCE_ID, mvsr -> mvsr.getMemberPkMap().get(AlignmentMember.SEQUENCE_ID_PATH)),
				column(PRESENT, mvsr -> mvsr.getVariationScanResult().isPresent()));
	}


}
