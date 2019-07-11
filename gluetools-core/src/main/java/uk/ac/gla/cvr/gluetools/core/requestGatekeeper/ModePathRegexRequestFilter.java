package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;

import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;

@PluginClass(elemName="modePathRegexFilter")
public class ModePathRegexRequestFilter extends BaseRequestFilter {

	private Pattern regex;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.regex = PluginUtils.configureRegexPatternProperty(configElem, "regex", true);
	}

	@Override
	protected boolean allowRequestLocal(Request request) {
		return regex.matcher(request.getModePath()).find();
	}
}
