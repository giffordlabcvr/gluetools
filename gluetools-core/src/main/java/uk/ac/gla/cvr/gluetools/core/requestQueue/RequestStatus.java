package uk.ac.gla.cvr.gluetools.core.requestQueue;

import org.apache.commons.lang.StringEscapeUtils;

import uk.ac.gla.cvr.gluetools.core.requestQueue.RequestTicket.Code;

public class RequestStatus {

	private String requestID;
	private RequestTicket.Code code;
	private int placeInQueue;
	private String runningDescription;
	
	public RequestStatus(String requestID, Code code, int placeInQueue, String runningDescription) {
		super();
		this.requestID = requestID;
		this.code = code;
		this.placeInQueue = placeInQueue;
		this.runningDescription = runningDescription;
	}

	public String getRequestID() {
		return requestID;
	}

	public RequestTicket.Code getCode() {
		return code;
	}

	public int getPlaceInQueue() {
		return placeInQueue;
	}

	public String getRunningDescription() {
		return runningDescription;
	}

	public String toJsonString() {
		StringBuffer buf = new StringBuffer();
		buf.append("{").append("\"requestID\":\""+StringEscapeUtils.escapeJavaScript(getRequestID())+"\"");
		buf.append(",").append("\"code\":\""+code.name()+"\"");
		if(code == Code.QUEUED) {
			buf.append(",").append("\"placeInQueue\":"+Integer.toString(getPlaceInQueue()));
		}
		if(code == Code.RUNNING) {
			buf.append(",").append("\"runningDescription\":\""+StringEscapeUtils.escapeJavaScript(getRunningDescription())+"\"");
		}
		buf.append("}");
		return buf.toString();
	}
	
}
