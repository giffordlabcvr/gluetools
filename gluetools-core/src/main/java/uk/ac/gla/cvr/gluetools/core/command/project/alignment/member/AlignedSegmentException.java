package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class AlignedSegmentException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		ALIGNED_SEGMENT_REF_REGION_OUT_OF_RANGE("alignmentName", "sourceName", "sequenceID", "refSeqLength", "refStart", "refEnd"),
		ALIGNED_SEGMENT_REF_REGION_ENDPOINTS_REVERSED("alignmentName", "sourceName", "sequenceID", "refStart", "refEnd"),
		ALIGNED_SEGMENT_MEMBER_REGION_OUT_OF_RANGE("alignmentName", "sourceName", "sequenceID", "membSeqLength", "memberStart", "memberEnd"),
		ALIGNED_SEGMENT_REGION_LENGTHS_NOT_EQUAL("alignmentName", "sourceName", "sequenceID", "refRegionLength", "membRegionLength"),
		;

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public AlignedSegmentException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public AlignedSegmentException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
