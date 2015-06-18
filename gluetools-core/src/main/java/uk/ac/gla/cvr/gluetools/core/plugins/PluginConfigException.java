package uk.ac.gla.cvr.gluetools.core.plugins;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class PluginConfigException extends GlueException {

	public enum Code implements GlueErrorCode {
		CONFIG_CONSTRAINT_VIOLATION("errorTxt"), 
		REQUIRED_CONFIG_MISSING("xPath"), 
		TOO_MANY_CONFIG_ELEMENTS("xPath", "numFound", "maximum"), 
		TOO_FEW_CONFIG_ELEMENTS("xPath", "numFound", "minimum"),
		CONFIG_FORMAT_ERROR("xPath", "errorTxt", "value"),
		UNKNOWN_CONFIG_ELEMENT("xPath"), 
		UNKNOWN_CONFIG_ATTRIBUTE("xPath");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
		
	}
	
	public PluginConfigException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PluginConfigException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
