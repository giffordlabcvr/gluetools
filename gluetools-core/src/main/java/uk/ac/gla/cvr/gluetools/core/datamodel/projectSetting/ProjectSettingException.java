package uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ProjectSettingException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		NO_SUCH_SETTING("settingName"),
		NO_SUCH_EXTENSION_SETTING("settingName"), 
		UNKNOWN_SETTING("settingName"),
		UNKNOWN_EXTENSION_SETTING("settingName"),
		INVALID_SETTING_VALUE("settingName", "badValue", "errorTxt"), 
		INVALID_EXTENSION_SETTING_VALUE("settingName", "badValue", "errorTxt");

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
