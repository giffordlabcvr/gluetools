package uk.ac.gla.cvr.gluetools.core.collation.sourcing;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class SequenceSourcerFactoryException extends GlueException {

	public enum Code implements GlueErrorCode {
		UNKNOWN_SOURCER_TYPE, 
		SOURCER_XML_MISSING_ELEMENT, 
		SOURCER_XML_MISSING_ATTRIBUTE, 
		SOURCER_CREATION_FAILED, 
		SOURCER_CONFIGURATION_FAILED
	}
	
	public SequenceSourcerFactoryException(Throwable cause, Code code, Object ... args) {
		super(cause, code, args);
	}

	public SequenceSourcerFactoryException(Code code, Object ... args) {
		super(code, args);
	}


}
