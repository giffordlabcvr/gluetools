package uk.ac.gla.cvr.gluetools.core;


public class GluetoolsEngineException extends GlueException {

	public enum Code implements GlueErrorCode {

		ENGINE_ALREADY_INITIALIZED(), 
		ENGINE_NOT_INITIALIZED(),
		CONFIG_INVALID_XML("configFilePath", "errorTxt"),
		CONFIG_ERROR("configFilePath", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public GluetoolsEngineException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public GluetoolsEngineException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
