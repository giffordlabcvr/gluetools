package uk.ac.gla.cvr.gluetools.core.plugins;

import freemarker.template.Configuration;

public class PluginConfigContext {

	private Configuration freemarkerConfiguration;

	public PluginConfigContext(Configuration freemarkerConfiguration) {
		super();
		this.freemarkerConfiguration = freemarkerConfiguration;
	}

	public Configuration getFreemarkerConfiguration() {
		return freemarkerConfiguration;
	}
	
}
