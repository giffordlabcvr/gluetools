package uk.ac.gla.cvr.gluetools.core.command.project.module;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ModuleCommandException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		CREATE_FROM_FILE_FAILED("file");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public ModuleCommandException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ModuleCommandException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
