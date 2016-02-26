package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class SequenceException extends GlueException {

	public enum Code implements GlueErrorCode {
		SEQUENCE_FORMAT_ERROR("errorText"),
		UNKNOWN_SEQUENCE_FORMAT("unknownFormat"),
		NO_DATA_PROVIDED(),
		BASE_64_FORMAT_EXCEPTION("errorText"),
		XML_SEQUENCE_DOES_NOT_CONTAIN_NUCLEOTIDES("primaryAccession"),
		UNABLE_TO_DETERMINE_SEQUENCE_FORMAT_FROM_BYTES, 
		UNABLE_TO_DETERMINE_SEQUENCE_FORMAT_FROM_FILE_EXTENSION("fileExtension");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public SequenceException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public SequenceException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
