package uk.ac.gla.cvr.gluetools.core.requestQueue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.requestQueue.RequestQueueManagerException.Code;

public class RequestQueueManager implements Plugin {

	/**
	 * Uncollected tickets removed from the map after a certain amount of time so that results get garbage collected.
	 * Clients should be polling for tickets more frequently.
	 */
	private static final long RETAIN_OUTSTANDING_TICKETS_LIMIT_MS = 30000;

	private static final long OUTSTANDING_TICKETS_THREAD_SLEEP_TIME_MS = 5000;

	private Map<String, RequestQueue> requestQueues = new LinkedHashMap<String, RequestQueue>();
	
	private Map<String, RequestTicket> uncollectedTickets = new LinkedHashMap<String, RequestTicket>();
	
	private boolean isInited;
	
	private int nextRequestID = 1;
	
	private Thread uncollectedTicketsThread;
	private boolean keepRunning = true;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		List<Element> requestQueueElems = PluginUtils.findConfigElements(configElem, "requestQueue");
		for(Element requestQueueElem: requestQueueElems) {
			RequestQueue requestQueue = new RequestQueue();
			PluginFactory.configurePlugin(pluginConfigContext, requestQueueElem, requestQueue);
			String queueName = requestQueue.getQueueName();
			if(requestQueues.containsKey(queueName)) {
				throw new RequestQueueManagerException(Code.CONFIG_ERROR, "Duplicate queue name: '"+queueName+"'");
			}
			requestQueues.put(queueName, requestQueue);
		}
	}

	public void addQueue(RequestQueue requestQueue) {
		requestQueues.put(requestQueue.getQueueName(), requestQueue);
	}
	
	public RequestQueue getQueue(String queueName) {
		return requestQueues.get(queueName);
	}
	
	public boolean isInited() {
		return isInited;
	}

	public void init() {
		for(RequestQueue requestQueue: requestQueues.values()) {
			requestQueue.init();
		}
		this.uncollectedTicketsThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(keepRunning) {
					synchronized(uncollectedTickets) {
						List<String> requestIDsToRemove = new ArrayList<String>();
						for(RequestTicket ticket: uncollectedTickets.values()) {
							if(ticket.getCommandFuture().isDone()) {
								Long completionTime = ticket.getCompletionTime();
								if(completionTime == null) {
									ticket.setCompletionTime(System.currentTimeMillis());
								} else {
									if(System.currentTimeMillis() - completionTime > RETAIN_OUTSTANDING_TICKETS_LIMIT_MS) {
										requestIDsToRemove.add(ticket.getId());
									}
								}
							}
						}
						for(String requestID: requestIDsToRemove) {
							GlueLogger.getGlueLogger().finest("Removing uncollected ticket for request "+requestID);
							uncollectedTickets.remove(requestID);
						}
					}
					try {
						Thread.sleep(OUTSTANDING_TICKETS_THREAD_SLEEP_TIME_MS);
					} catch (InterruptedException e) {}
				}
			}
			
		}, "Request queue manager uncollected tickets thread");
		this.uncollectedTicketsThread.start();
		this.isInited = true;
	}
	
	public void dispose() {
		for(RequestQueue requestQueue: requestQueues.values()) {
			requestQueue.dispose();
		}
		this.keepRunning = false;
		try {
			this.uncollectedTicketsThread.join();
		} catch (InterruptedException e) {}
	}
	
	public RequestStatus submitRequest(CommandContext cmdContext, Request request) {
		String queueName = request.getQueueName();
		RequestQueue requestQueue = getQueue(queueName);
		if(requestQueue == null) {
			throw new RequestQueueManagerException(RequestQueueManagerException.Code.QUEUE_ASSIGNMENT_ERROR, 
					"Request was assigned to queue '"+queueName+"' but no queue with this name has been configured");
		}

		final RequestTicket requestTicket;
		String requestID;
		synchronized(uncollectedTickets) {
			Map<String, RequestTicket> queuedTickets = requestQueue.getQueuedTickets();
			int currentLengthOfQueue = queuedTickets.values().size();
			if(currentLengthOfQueue >= requestQueue.getMaxRequests()) {
				throw new RequestQueueManagerException(Code.QUEUE_FULL, "Request rejected from queue '"+requestQueue.getQueueName()+
						"', this queue is at its maximum load of "+requestQueue.getMaxRequests()+". Please try again later.");
			}
			requestID = Integer.toString(nextRequestID);
			nextRequestID++;
			ExecutorService executorService = requestQueue.getExecutorService();
			requestTicket = new RequestTicket(requestID, cmdContext);
			Map<String, RequestTicket> runningTickets = requestQueue.getRunningTickets();
			if(runningTickets.size() < requestQueue.getNumWorkers()) {
				// request should be immediately run so don't return QUEUED unnecessarily
				requestTicket.setCode(RequestTicket.Code.RUNNING); 
				requestTicket.setPlaceInQueue(-1); 
				runningTickets.put(requestID, requestTicket);
			} else {
				requestTicket.setCode(RequestTicket.Code.QUEUED);
				requestTicket.setPlaceInQueue(currentLengthOfQueue); 
				queuedTickets.put(requestID, requestTicket);
			}
			uncollectedTickets.put(requestID, requestTicket);

			Future<CommandResult> cmdResultFuture = executorService.submit(new Callable<CommandResult>() {
				@Override
				public CommandResult call() throws Exception {
					CommandResult cmdResult;
					try {
						cmdResult = GluetoolsEngine.getInstance().runWithGlueClassloader(new Supplier<CommandResult>(){
							@Override
							public CommandResult get() {
								GlueLogger.getGlueLogger().info("Executing request "+requestID+" on queue '"+queueName+"'");
								Map<String, RequestTicket> queuedTickets = requestQueue.getQueuedTickets();
								if(queuedTickets.remove(requestID) != null) {
									// request may have gone straight into runningTickets map.
									Map<String, RequestTicket> runningTickets = requestQueue.getRunningTickets();
									runningTickets.put(requestID, requestTicket);
									for(RequestTicket queuedTicket: queuedTickets.values()) {
										queuedTicket.decrementPlaceInQueue();
									}
									requestTicket.setCode(RequestTicket.Code.RUNNING);
									requestTicket.setPlaceInQueue(-1);
								}
								return request.getCommand().execute(cmdContext);
							}
						});
					} finally {
						requestQueue.getRunningTickets().remove(requestID);
						requestTicket.setCode(RequestTicket.Code.COMPLETE);
						cmdContext.dispose();
					}
					return cmdResult;
				}
			});
			requestTicket.setCmdResultFuture(cmdResultFuture);
		}
		return requestStatus(requestID);
	}
	
	public CommandResult collectRequestSync(String id) {
		RequestTicket requestTicket;
		synchronized(uncollectedTickets) {
			requestTicket = uncollectedTickets.remove(id);
		}
		if(requestTicket == null) {
			throw new RequestQueueManagerException(RequestQueueManagerException.Code.EXPIRED_OR_NON_EXISTENT_REQUEST, 
					"Request with ID "+id+" is expired or non-existent.");
		}
		return requestTicket.getCommandResult();
	}
	
	public RequestStatus requestStatus(String id) {
		RequestTicket requestTicket;
		synchronized(uncollectedTickets) {
			requestTicket = uncollectedTickets.get(id);
			if(requestTicket == null) {
				throw new RequestQueueManagerException(RequestQueueManagerException.Code.EXPIRED_OR_NON_EXISTENT_REQUEST, 
						"Request with ID "+id+" is expired or non-existent.");
			}
			return new RequestStatus(id, requestTicket.getCode(), requestTicket.getPlaceInQueue(), requestTicket.getRunningDescription());
		}
	}
}
