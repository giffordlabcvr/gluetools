package uk.ac.gla.cvr.gluetools.core.collation.sequence.gbflatfile;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class GenbankFlatFileException extends GlueException {

	public enum Code implements GlueErrorCode {
		COMPOUND_NOT_FOUND("errorTxt"), 
		GENBANK_PARSING_FAILED, 
		MULTIPLE_GENBANK_FILES_PARSED;
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public GenbankFlatFileException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public GenbankFlatFileException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
