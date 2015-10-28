package uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class VariationCategoryException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		PARENT_RELATIONSHIP_LOOP("loopNames"),
		;

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public VariationCategoryException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public VariationCategoryException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
