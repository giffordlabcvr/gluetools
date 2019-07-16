package uk.ac.gla.cvr.gluetools.core.requestQueue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;

public class RequestTicket {

	private String id;
	private Future<CommandResult> commandFuture;
	private Long completionTime;
	
	public RequestTicket(String id, Future<CommandResult> commandFuture) {
		super();
		this.id = id;
		this.commandFuture = commandFuture;
	}
	
	public String getId() {
		return id;
	}
	
	public Future<CommandResult> getCommandFuture() {
		return commandFuture;
	}
	
	public Long getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(Long completionTime) {
		this.completionTime = completionTime;
	}

	public CommandResult getCommandResult() {
		try {
			return commandFuture.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if(cause instanceof GlueException) {
				throw ((GlueException) cause);
			}
			throw new RequestQueueManagerException(cause, RequestQueueManagerException.Code.REQUEST_ERROR, cause.getLocalizedMessage());
		} catch (InterruptedException e) {
			throw new RequestQueueManagerException(e, RequestQueueManagerException.Code.REQUEST_INTERRUPTED, e.getLocalizedMessage());
		} catch (CancellationException e) {
			throw new RequestQueueManagerException(e, RequestQueueManagerException.Code.REQUEST_CANCELLED, e.getLocalizedMessage());
		}
	}
}
