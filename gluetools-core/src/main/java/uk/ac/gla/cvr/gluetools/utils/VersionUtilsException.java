package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class VersionUtilsException extends GlueException {

	public enum Code implements GlueErrorCode {

		CANNOT_EXTRACT_NUMBERS_FROM_VERSION_STRING("versionString"),
		GLUE_ENGINE_VERSION_LATER_THAN_PROJECT_MAX("glueEngineVersion", "projectMaxVersion"),
		GLUE_ENGINE_VERSION_EARLIER_THAN_PROJECT_MIN("glueEngineVersion", "projectMinVersion");

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
