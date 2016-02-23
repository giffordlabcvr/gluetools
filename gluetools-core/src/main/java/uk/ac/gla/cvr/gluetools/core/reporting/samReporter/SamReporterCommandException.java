package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class SamReporterCommandException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		NO_GLUE_REFERENCE_DEFINED(), 
		TIP_ALIGNMENT_MEMBER_NOT_FOUND("samReferenceName", "whereClause"),
		TIP_ALIGNMENT_MEMBER_EXTRACTOR_FAILED("samReferenceName"),
		AMBIGUOUS_TIP_ALIGNMENT_MEMBER_DEFINED();

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
