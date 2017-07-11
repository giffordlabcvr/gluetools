package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class EcmaFunctionInvokerException extends GlueException {

	public enum Code implements GlueErrorCode {
		FUNCTION_NAME_UNKNOWN("unknownFunctionName", "moduleName"),
		INCORRECT_NUMBER_OF_ARGUMENTS("moduleName", "functionName", "expectedNumArguments", "actualNumArguments"), 
		FUNCTION_LOOKUP_EXCEPTION("moduleName", "functionName", "errorTxt"), 
		FUNCTION_INVOCATION_EXCEPTION("moduleName", "functionName", "errorTxt"),
		FUNCTION_RESULT_EXCEPTION("moduleName", "functionName", "errorTxt"),
		INVALID_CONFIG_DOCUMENT("moduleName", "errorTxt");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
		
	}
	
	public EcmaFunctionInvokerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public EcmaFunctionInvokerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}


	
}
