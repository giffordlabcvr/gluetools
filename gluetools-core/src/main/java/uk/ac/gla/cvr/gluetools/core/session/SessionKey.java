package uk.ac.gla.cvr.gluetools.core.session;

import java.util.Arrays;

public class SessionKey {

	private String sessionType;
	private String[] sessionArgs;
	
	public SessionKey(String sessionType, String[] sessionArgs) {
		super();
		this.sessionType = sessionType;
		this.sessionArgs = sessionArgs;
	}

	public String getSessionType() {
		return sessionType;
	}

	public String[] getSessionArgs() {
		return sessionArgs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(sessionArgs);
		result = prime * result + ((sessionType == null) ? 0 : sessionType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SessionKey other = (SessionKey) obj;
		if (!Arrays.equals(sessionArgs, other.sessionArgs))
			return false;
		if (sessionType == null) {
			if (other.sessionType != null)
				return false;
		} else if (!sessionType.equals(other.sessionType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SessionKey [sessionType=" + sessionType + ", sessionArgs=" + Arrays.toString(sessionArgs) + "]";
	}
	
	
}
