package uk.ac.gla.cvr.gluetools.core.requestQueue;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class RequestQueue implements Plugin {

	private String queueName;
	private int numWorkers;

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
	
}
