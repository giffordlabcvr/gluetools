package uk.ac.gla.cvr.gluetools.core.command.render;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class RenderObjectException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		NO_DEFAULT_RENDERER_DEFINED("class"),
		ERROR_LOADING_DEFAULT_RENDERER_TEMPLATE("resource", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public RenderObjectException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public RenderObjectException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}