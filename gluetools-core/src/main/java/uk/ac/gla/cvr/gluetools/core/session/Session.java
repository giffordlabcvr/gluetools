package uk.ac.gla.cvr.gluetools.core.session;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.session.SessionException.Code;

public abstract class Session {

	public abstract void init(CommandContext cmdContext, String[] sessionArgs);

	public abstract void close();

	protected String getSessionArgString(String[] sessionArgs, String argName, int index, boolean required) {
		if(index >= sessionArgs.length) {
			if(required) {
				throw new SessionException(Code.SESSION_CREATION_ERROR, "Not enough session arguments: expected at least "+(index+1));
			} else {
				return null;
			}
		} else {
			String sessionArg = sessionArgs[index];
			if(sessionArg == null && required) {
				throw new SessionException(Code.SESSION_CREATION_ERROR, "Required session argument "+argName+" (position "+index+") was null");
			}
			return sessionArg;
		}
	}
	
}
