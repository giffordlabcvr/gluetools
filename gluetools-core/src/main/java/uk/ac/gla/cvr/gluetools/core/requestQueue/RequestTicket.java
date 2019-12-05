package uk.ac.gla.cvr.gluetools.core.requestQueue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public class RequestTicket {

	private String id;
	private Future<CommandResult> cmdResultFuture;
	private Long startTime;
	private Long completionTime;
	
	public enum Code {
		QUEUED,
		RUNNING,
		COMPLETE,
	}
	
	private Code code = Code.QUEUED;
	private int placeInQueue;
	private CommandContext cmdContext;
	
	public RequestTicket(String id, CommandContext cmdContext) {
		super();
		this.id = id;
		this.cmdContext = cmdContext;
	}
	
	public String getId() {
		return id;
	}
	
	public Future<CommandResult> getCommandFuture() {
		return cmdResultFuture;
	}
	
	public synchronized Long getCompletionTime() {
		return completionTime;
	}

	public synchronized void setCompletionTime(Long completionTime) {
		this.completionTime = completionTime;
	}

	public synchronized Long getStartTime() {
		return startTime;
	}

	public synchronized void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public CommandResult getCommandResult() {
		try {
			return cmdResultFuture.get();
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if(cause instanceof GlueException) {
				throw ((GlueException) cause);
			}
			GlueLogger.getGlueLogger().log(Level.SEVERE, "Non-GLUE exception: "+cause.getLocalizedMessage(), cause);
			throw new RequestQueueManagerException(cause, RequestQueueManagerException.Code.REQUEST_ERROR, cause.getLocalizedMessage());
		} catch (InterruptedException e) {
			throw new RequestQueueManagerException(e, RequestQueueManagerException.Code.REQUEST_INTERRUPTED, e.getLocalizedMessage());
		} catch (CancellationException e) {
			throw new RequestQueueManagerException(e, RequestQueueManagerException.Code.REQUEST_CANCELLED, e.getLocalizedMessage());
		}
	}

	public synchronized Code getCode() {
		return code;
	}

	public synchronized int getPlaceInQueue() {
		return placeInQueue;
	}

	public synchronized void setCode(Code code) {
		this.code = code;
	}

	public synchronized void setPlaceInQueue(int placeInQueue) {
		this.placeInQueue = placeInQueue;
	}

	public synchronized void decrementPlaceInQueue() {
		this.placeInQueue--;
	}

	public String getRunningDescription() {
		return cmdContext.getRunningDescription();
	}

	public void setCmdResultFuture(Future<CommandResult> cmdResultFuture) {
		this.cmdResultFuture = cmdResultFuture;
	}
}
