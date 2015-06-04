package uk.ac.gla.cvr.gluetools.core.plugins;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class PluginFactoryException extends GlueException {

	public enum Code implements GlueErrorCode {
		UNKNOWN_ELEMENT_NAME, PLUGIN_CREATION_FAILED
		
	}
	
	public PluginFactoryException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PluginFactoryException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
