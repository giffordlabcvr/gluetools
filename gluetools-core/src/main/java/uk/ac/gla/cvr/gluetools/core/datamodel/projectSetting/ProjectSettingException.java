package uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ProjectSettingException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		NO_SUCH_SETTING("settingName"),
		INVALID_SETTING_VALUE("settingName", "badValue", "errorTxt"), 
		UNKNOWN_SETTING("settingName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}

	public ProjectSettingException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ProjectSettingException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
