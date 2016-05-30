package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import freemarker.template.Template;

public class PropertyTemplate implements Plugin {

	public static final String NAME = "name";
	public static final String TEMPLATE = "template";
	
	private String name;
	private Template template;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		name = PluginUtils.configureStringProperty(configElem, NAME, true);
		template = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, TEMPLATE, false);
	}

	public String getName() {
		return name;
	}

	public Template getTemplate() {
		return template;
	}
	
}
