package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;

@PluginClass(elemName="queueAssignmentFilter")
public class QueueAssignmentRequestFilter extends BaseRequestFilter {

	private String queueName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.queueName = PluginUtils.configureStringProperty(configElem, "queueName", true);
	}
	
	@Override
	protected boolean allowRequestLocal(Request request) {
		request.setQueueName(queueName);
		return true;
	}

}
