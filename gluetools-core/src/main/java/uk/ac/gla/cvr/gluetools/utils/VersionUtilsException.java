package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class VersionUtilsException extends GlueException {

	public enum Code implements GlueErrorCode {

		VERSION_STRING_INCORRECT_FORMAT("versionString"),
		GLUE_ENGINE_VERSION_LATER_THAN_PROJECT_MAX("glueEngineVersion", "projectMaxVersion"),
		GLUE_ENGINE_VERSION_EARLIER_THAN_PROJECT_MIN("glueEngineVersion", "projectMinVersion"),
		PROJECT_MIN_VERSION_ALREADY_SET("projectMinVersion"),
		PROJECT_MAX_VERSION_ALREADY_SET("projectMaxVersion");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public VersionUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public VersionUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
