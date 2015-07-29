package uk.ac.gla.cvr.gluetools.ws;

@SuppressWarnings("serial")
public class GlueApplicationException extends RuntimeException {

	public GlueApplicationException(String message, Throwable cause) {
		super(message, cause);
	}

	public GlueApplicationException(String message) {
		super(message);
	}

}
