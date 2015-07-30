package uk.ac.gla.cvr.gluetools.core;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

@SuppressWarnings("serial")
public abstract class GlueException extends RuntimeException {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");
	
	public interface GlueErrorCode {
		public String name();
		public String[] getArgNames();
	}
	
	private GlueErrorCode code;
	private Object[] errorArgs;
	
	protected GlueException(GlueErrorCode code, Object ... errorArgs) {
		super();
		if(code == null) { throw new IllegalArgumentException("Error code must be supplied."); }
		this.code = code;
		this.errorArgs = errorArgs;
		checkArgs(code, errorArgs);
	}

	protected GlueException(Throwable cause, GlueErrorCode code, Object ... errorArgs) {
		super(cause);
		if(code == null) { throw new IllegalArgumentException("Error code must be supplied."); }
		this.code = code;
		this.errorArgs = errorArgs;
		checkArgs(code, errorArgs);
	}

	private void checkArgs(GlueErrorCode code, Object... errorArgs) {
		int suppliedArgs = errorArgs.length;
		int requiredArgs = code.getArgNames().length;
		if(suppliedArgs != requiredArgs) {
			logger.warning(this.getClass().getSimpleName()+"."+code.name()+
					" supplied args: "+suppliedArgs+", required args: "+requiredArgs);
		}
	}
	
	public String getMessage() {
		String messageCode = "UNKNOWN_ERROR_CODE";
		String codeName = code.name();
		String[] argNames = code.getArgNames();
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
					if(errorArgs != null && errorArgs.length != argNames.length) {
						logger.warning("Wrong number of error arguments for "+messageCode+" in "+
								getClass().getSimpleName()+", args: "+Arrays.asList(errorArgs));
					} else {
						try {
							return MessageFormat.format(messagePattern, errorArgs);
						} catch(Throwable th) {
							logger.warning("MessageFormat.format() failed for "+messageCode+" in "+
									getClass().getSimpleName()+", args: "+Arrays.asList(errorArgs));
						}
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
		String errorArgsString;
		if(errorArgs == null) {
			errorArgsString = "";
		} else {
			if(errorArgs.length == argNames.length) {
				List<String> argStrings = IntStream.range(0, errorArgs.length).
					mapToObj(i -> argNames[i]+"="+errorArgs[i]).collect(Collectors.toList());
				errorArgsString = "["+String.join(", ", argStrings)+"]";
			} else {
				errorArgsString = Arrays.asList(errorArgs).toString();
			}
		}
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

	public GlueErrorCode getCode() {
		return code;
	}
	
	public Object[] getErrorArgs() {
		return errorArgs;
	}

	public JsonObject toJsonObject() {
		JsonObjectBuilder detailBuilder = JsonUtils.jsonObjectBuilder();
		detailBuilder.add("message", getMessage());
		detailBuilder.add("code", getCode().name());
		for(int i = 0; i < errorArgs.length; i++) {
			detailBuilder.add(getCode().getArgNames()[i], errorArgs[i].toString());
		}
		Throwable cause = getCause();
		if(cause instanceof GlueException) {
			detailBuilder.add("cause", ((GlueException) cause).toJsonObject());
		}
		
		JsonObjectBuilder builder = JsonUtils.jsonObjectBuilder();
		builder.add(getClass().getSimpleName(), detailBuilder);
		return builder.build();
	}
	
}
