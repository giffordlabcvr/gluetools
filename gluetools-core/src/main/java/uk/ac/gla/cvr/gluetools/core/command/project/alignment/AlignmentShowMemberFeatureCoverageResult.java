package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class AlignmentShowMemberFeatureCoverageResult extends BaseTableResult<MemberFeatureCoverage> {


	public static final String 
		ALIGNMENT_NAME = "alignmentName",
		SOURCE_NAME = "sourceName",
		SEQUENCE_ID = "sequenceID",
		REFERENCE_NT_COVERAGE = "referenceNtCoverage";


	public AlignmentShowMemberFeatureCoverageResult(List<MemberFeatureCoverage> rowData) {
		super("alignmentShowMemberFeatureCoverageResult", rowData,
				column(ALIGNMENT_NAME, mfc -> mfc.getAlignmentMember().getAlignment().getName()),
				column(SOURCE_NAME, mfc -> mfc.getAlignmentMember().getSequence().getSource().getName()),
				column(SEQUENCE_ID, mfc -> mfc.getAlignmentMember().getSequence().getSequenceID()),
				column(REFERENCE_NT_COVERAGE, mfc -> mfc.getFeatureReferenceNtCoverage()));
	}


}
