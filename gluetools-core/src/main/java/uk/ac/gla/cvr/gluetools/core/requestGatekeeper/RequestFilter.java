package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;

public interface RequestFilter extends Plugin {

	public boolean filterRequest(Request request);
	
}
