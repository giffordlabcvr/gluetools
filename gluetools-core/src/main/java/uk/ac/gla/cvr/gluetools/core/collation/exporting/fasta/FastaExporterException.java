package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FastaExporterException extends GlueException {

	public enum Code implements GlueErrorCode {
		CANNOT_SPECIFY_FEATURE_FOR_UNCONSTRAINED_ALIGNMENT("alignmentName"),
		CANNOT_SPECIFY_RECURSIVE_FOR_UNCONSTRAINED_ALIGNMENT("alignmentName"),
		CANNOT_SPECIFY_INCLUDE_ALL_COLUMNS_FOR_UNCONSTRAINED_ALIGNMENT("alignmentName");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public FastaExporterException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FastaExporterException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
