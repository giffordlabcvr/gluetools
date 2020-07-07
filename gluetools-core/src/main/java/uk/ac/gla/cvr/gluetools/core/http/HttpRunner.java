package uk.ac.gla.cvr.gluetools.core.http;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="httpRunner",
description="Runs HTTP requests")
public class HttpRunner extends ModulePlugin<HttpRunner> {

	// base URL
	private static final String BASE_URL = "baseUrl";
	
	private String baseUrl;

	public HttpRunner() {
		super();
		addSimplePropertyName(BASE_URL);
		registerModulePluginCmdClass(HttpRunnerGetCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		String baseUrl = PluginUtils.configureStringProperty(configElem, BASE_URL, true);
		if(baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length()-1);
		}
		this.baseUrl = baseUrl;

	}

	public String getBaseUrl() {
		return baseUrl;
	}
	
}
