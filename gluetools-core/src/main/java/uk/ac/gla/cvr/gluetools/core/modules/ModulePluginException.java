package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public abstract class ModulePluginException extends GlueException {

	protected ModulePluginException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	protected ModulePluginException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
