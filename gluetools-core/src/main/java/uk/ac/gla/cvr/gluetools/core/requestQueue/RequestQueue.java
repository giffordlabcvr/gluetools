package uk.ac.gla.cvr.gluetools.core.requestQueue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class RequestQueue implements Plugin {

	// only queue that exists if no queue manager is defined. 
	// also, queue that requests are assigned to if no explicit assignment is made.
	public static final String DEFAULT_QUEUE_NAME = "default";
	
	private String queueName;
	private int numWorkers;
	private int maxRequests;
	
	private ExecutorService executorService;

	private Map<String, RequestTicket> queuedTickets = new LinkedHashMap<String, RequestTicket>();
	private Map<String, RequestTicket> runningTickets = new LinkedHashMap<String, RequestTicket>();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.queueName = PluginUtils.configureStringProperty(configElem, "queueName", true);
		this.numWorkers = PluginUtils.configureIntProperty(configElem, "numWorkers", true);
		this.maxRequests = PluginUtils.configureIntProperty(configElem, "maxRequests", true);
	}

	public Map<String, RequestTicket> getQueuedTickets() {
		return queuedTickets;
	}

	public Map<String, RequestTicket> getRunningTickets() {
		return runningTickets;
	}

	public String getQueueName() {
		return queueName;
	}

	public int getNumWorkers() {
		return numWorkers;
	}

	public int getMaxRequests() {
		return maxRequests;
	}

	public RequestQueue() {
		super();
	}

	public RequestQueue(String queueName, int numWorkers, int maxRequests) {
		super();
		this.queueName = queueName;
		this.numWorkers = numWorkers;
		this.maxRequests = maxRequests;
	}
	
	public void init() {
		this.executorService = new ThreadPoolExecutor(numWorkers, numWorkers,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
	}
	
	public void dispose() {
		if(this.executorService != null) {
			this.executorService.shutdownNow();
		}
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	
	
}
