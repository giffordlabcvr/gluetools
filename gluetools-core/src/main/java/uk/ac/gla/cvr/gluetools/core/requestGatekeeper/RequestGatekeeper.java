package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;

import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;

/*
 * Each GLUE engine instance has zero or one instances of RequestGatekeeper.
 * The request gatekeeper determines which requests may be invoked by a client, 
 * (based on matching the mode path, command words and command arguments).
 * and when they are, which RequestQueue they are placed in.
 * 
 */
public class RequestGatekeeper extends BaseRequestFilter {

	@Override
	protected boolean fiterRequestInternal(Request request) {
		return true;
	}
	
}
