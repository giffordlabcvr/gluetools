package uk.ac.gla.cvr.gluetools.ws;

import javax.ws.rs.Path;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;

@Path("/")
public class GlueRequestHandler {

	public GlueRequestHandler() {
	}

	
	@Path("/")
	public Object handleRequest() {
		WsCmdContext cmdContext = new WsCmdContext(GluetoolsEngine.getInstance());
		return cmdContext;
	}
	

}
