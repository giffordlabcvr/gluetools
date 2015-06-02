package uk.ac.gla.cvr.gluetools.core;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public abstract class GlueException extends Exception {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");
	
	protected interface GlueErrorCode {
		public String name();
	}
	
	private GlueErrorCode code;
	private Object[] errorArgs;
	
	protected GlueException(GlueErrorCode code, Object ... errorArgs) {
		super();
		if(code == null) { throw new IllegalArgumentException("Error code must be supplied."); }
		this.code = code;
		this.errorArgs = errorArgs;
	}

	protected GlueException(Throwable cause, GlueErrorCode code, Object ... errorArgs) {
		super(cause);
		if(code == null) { throw new IllegalArgumentException("Error code must be supplied."); }
		this.code = code;
		this.errorArgs = errorArgs;
	}
	
	public String getMessage() {
		String messageCode = "UNKNOWN_ERROR_CODE";
		String codeName = code.name();
		if(codeName != null) {
			messageCode = codeName;
			ResourceBundle resourceBundle = getResourceBundle();
			if(resourceBundle != null) {
				String messagePattern = null;
				try {
					messagePattern = resourceBundle.getString(messageCode);
				} catch(MissingResourceException mre) {
					logger.warning(mre.getMessage());
				}
				if(messagePattern != null) {
					try {
						return MessageFormat.format(messagePattern, errorArgs);
					} catch(Throwable th) {
						logger.warning("MessageFormat.format() failed for "+messageCode+" in "+
								getClass().getSimpleName()+", args: "+errorArgs);
					}
				} else {
					logger.warning("No key found for error code "+messageCode+" in "+getClass().getSimpleName());
				}
			} else {
				logger.warning("Error code name was null for "+getClass().getSimpleName());
			}
		} else {
			logger.warning("No ResourceBundle for "+getClass().getSimpleName());
		}
		String errorArgsString = errorArgs == null ? "": Arrays.asList(errorArgs).toString(); 
		return messageCode+errorArgsString;
	}

	private ResourceBundle getResourceBundle() {
		try {
			return ResourceBundle.getBundle(getClass().getSimpleName(), Locale.getDefault());
		} catch(MissingResourceException mre) {
			logger.warning(mre.getMessage());
			return null;
		}
	}
}
