package uk.ac.gla.cvr.gluetools.ws;

import javax.ws.rs.Path;

import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

public class GlueRequestHandler {

	public GlueRequestHandler() {
	}
	
	@Path("/handleRequest")
	public Object handleRequest() {
		WsCmdContext cmdContext = new WsCmdContext(GluetoolsEngine.getInstance());
		ServerRuntime rootServerRuntime = cmdContext.getGluetoolsEngine().getRootServerRuntime();
		RootCommandMode rootCommandMode = new RootCommandMode(rootServerRuntime);
		cmdContext.setObjectContext(GlueDataObject.createObjectContext(rootServerRuntime));
		cmdContext.pushCommandMode(rootCommandMode);
		return cmdContext;
	}
	

}
