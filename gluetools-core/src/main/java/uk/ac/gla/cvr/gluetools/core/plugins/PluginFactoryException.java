package uk.ac.gla.cvr.gluetools.core.plugins;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class PluginFactoryException extends GlueException {

	public enum Code implements GlueErrorCode {
		PLUGIN_CONFIG_FORMAT_ERROR("errorTxt"), 
		UNKNOWN_ELEMENT_NAME("factory", "elementName"), 
		PLUGIN_CREATION_FAILED("class");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
		
	}
	
	public PluginFactoryException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PluginFactoryException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
