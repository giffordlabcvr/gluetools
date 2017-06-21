package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class SamReporterCommandException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		NO_TARGET_REFERENCE_DEFINED(),
		NO_PLACEMENT_NEIGHBOURS_FOUND("cutoffDistance"),
		NO_CONSENSUS_PLACEMENTS(),
		TARGET_REFERENCE_NOT_FOUND("samReferenceName"),
		TARGET_REFERENCE_AMBIGUOUS("samReferenceName", "targetRefNames"),
		NO_TIP_ALIGNMENT_DEFINED(),
		TIP_ALIGNMENT_NOT_FOUND("samReferenceName"),
		TIP_ALIGNMENT_AMBIGUOUS("samReferenceName", "tipAlmtNames"),
		NO_SAM_CONSENSUS("minQScore", "minDepth"),
		ILLEGAL_SAM_REF_SENSE("illegalSamRefSense", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected SamReporterCommandException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public SamReporterCommandException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
}
