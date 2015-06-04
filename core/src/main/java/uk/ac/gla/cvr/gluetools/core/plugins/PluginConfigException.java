package uk.ac.gla.cvr.gluetools.core.plugins;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class PluginConfigException extends GlueException {

	public enum Code implements GlueErrorCode {
		REQUIRED_CONFIG_MISSING, 
		TOO_MANY_CONFIG_ELEMENTS, 
		TOO_FEW_CONFIG_ELEMENTS,
		CONFIG_FORMAT_ERROR
		
	}
	
	public PluginConfigException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PluginConfigException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
