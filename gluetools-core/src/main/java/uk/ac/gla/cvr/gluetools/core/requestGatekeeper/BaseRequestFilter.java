package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class BaseRequestFilter implements RequestFilter {

	private List<RequestFilter> childRequestFilters = new ArrayList<RequestFilter>();
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		RequestFilterFactory requestFilterFactory = PluginFactory.get(RequestFilterFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(requestFilterFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		childRequestFilters = requestFilterFactory.createFromElements(pluginConfigContext, ruleElems);	
	}

	@Override
	public final boolean filterRequest(Request request) {
		boolean locallyAllowed = fiterRequestInternal(request);
		if(locallyAllowed) {
			for(RequestFilter childRequestFilter: childRequestFilters) {
				boolean childAllowed = childRequestFilter.filterRequest(request);
				if(childAllowed) {
					return true;
				}
			}
		}
		return false;
	}

	protected abstract boolean fiterRequestInternal(Request request);

	
	
}
