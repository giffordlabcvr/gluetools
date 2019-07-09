package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;

@PluginClass(elemName="modePathFilter")
public class ModePathRequestFilter extends BaseRequestFilter {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}

	@Override
	protected boolean fiterRequestInternal(Request request) {
		return false;
	}

}
