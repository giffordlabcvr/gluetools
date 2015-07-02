package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FastaImporterException extends GlueException {

	public enum Code implements GlueErrorCode {
		NULL_IDENTIFIER("fastaHeader");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public FastaImporterException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FastaImporterException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
