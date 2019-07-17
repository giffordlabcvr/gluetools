package uk.ac.gla.cvr.gluetools.ws;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.requestQueue.RequestQueueManager;
import uk.ac.gla.cvr.gluetools.core.requestQueue.RequestStatus;

public class AsyncHandler {

	@Produces(MediaType.APPLICATION_JSON)
	@Path("/request-status/{requestID}")
	@GET
	public String requestStatus(@PathParam("requestID") String requestID, @Context HttpServletResponse response) {
		RequestQueueManager requestQueueManager = GluetoolsEngine.getInstance().getRequestQueueManager();
		RequestStatus requestStatus = requestQueueManager.requestStatus(requestID);
		WsCmdContext.addCacheDisablingHeaders(response);
		return requestStatus.toJsonString();
	}
	
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/collect-result/{requestID}")
	@GET
	public String collectResult(@PathParam("requestID") String requestID, @Context HttpServletResponse response) {
		RequestQueueManager requestQueueManager = GluetoolsEngine.getInstance().getRequestQueueManager();
		CommandResult cmdResult = requestQueueManager.collectRequestSync(requestID);
		String resultString = WsCmdContext.serializeToJson(cmdResult);
		WsCmdContext.addCacheDisablingHeaders(response);
		return resultString;
	}

}
