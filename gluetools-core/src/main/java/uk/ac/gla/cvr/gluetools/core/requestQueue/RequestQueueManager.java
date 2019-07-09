package uk.ac.gla.cvr.gluetools.core.requestQueue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.requestQueue.RequestQueueManagerException.Code;

public class RequestQueueManager implements Plugin {

	private Map<String, RequestQueue> requestQueues = new LinkedHashMap<String, RequestQueue>();
	
	
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

	
	
}
