package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class Kuiken2006CodonLabelerException extends GlueException {

	public enum Code implements GlueErrorCode {

		GAP_AT_START("refName", "ntLocation");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected Kuiken2006CodonLabelerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public Kuiken2006CodonLabelerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
	
}
