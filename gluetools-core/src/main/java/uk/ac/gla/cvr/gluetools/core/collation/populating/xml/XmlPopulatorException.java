package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class XmlPopulatorException extends GlueException {

	public enum Code implements GlueErrorCode {
		INCOMPATIBLE_SEQUENCE_FORMAT("pkMap", "incompatibleFormat"), 
		POPULATOR_RULE_FAILED("errorTxt"), 
		POPULATOR_CHILD_RULE_FAILED("errorTxt"), 
		SEQUENCE_INCORRECTLY_FORMATTED("pkMap", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}

	public XmlPopulatorException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public XmlPopulatorException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
	
}
