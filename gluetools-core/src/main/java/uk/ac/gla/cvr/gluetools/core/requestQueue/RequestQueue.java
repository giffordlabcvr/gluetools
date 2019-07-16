package uk.ac.gla.cvr.gluetools.core.requestQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	
	private ExecutorService executorService;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.queueName = PluginUtils.configureStringProperty(configElem, "queueName", true);
		this.numWorkers = PluginUtils.configureIntProperty(configElem, "numWorkers", true);
	}

	public String getQueueName() {
		return queueName;
	}

	public int getNumWorkers() {
		return numWorkers;
	}

	public RequestQueue() {
		super();
	}

	public RequestQueue(String queueName, int numWorkers) {
		super();
		this.queueName = queueName;
		this.numWorkers = numWorkers;
	}
	
	public void init() {
		this.executorService = Executors.newFixedThreadPool(numWorkers);
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
