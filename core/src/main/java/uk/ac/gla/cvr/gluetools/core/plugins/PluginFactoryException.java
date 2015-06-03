package uk.ac.gla.cvr.gluetools.core.plugins;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class PluginFactoryException extends GlueException {

	public enum Code implements GlueErrorCode {
		INCORRECT_ROOT_ELEMENT, MISSING_TYPE_ATTRIBUTE, UNKNOWN_PLUGIN_TYPE, PLUGIN_CREATION_FAILED, PLUGIN_CONFIGURATION_FAILED
		
	}
	
	public PluginFactoryException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PluginFactoryException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
