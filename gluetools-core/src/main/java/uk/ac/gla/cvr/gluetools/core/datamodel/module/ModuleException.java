package uk.ac.gla.cvr.gluetools.core.datamodel.module;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ModuleException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		CREATE_FROM_FILE_FAILED("file", "errorTxt"),
		NO_SUCH_MODULE_TYPE("moduleType"),
		NO_SUCH_MODULE_PROPERTY("propertyName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public ModuleException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ModuleException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
