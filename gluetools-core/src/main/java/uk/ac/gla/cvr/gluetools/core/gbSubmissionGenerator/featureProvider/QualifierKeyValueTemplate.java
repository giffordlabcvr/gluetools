package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;

public class QualifierKeyValueTemplate implements Plugin {

	public static final String KEY = "key";
	public static final String VALUE_TEMPLATE = "valueTemplate";
	
	private String key;
	private Template valueTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.key = PluginUtils.configureStringProperty(configElem, KEY, true);
		this.valueTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, VALUE_TEMPLATE, true);
	}

	public String getKey() {
		return key;
	}
	
	public String generateValueFromFeatureLocation(CommandContext cmdContext, FeatureLocation featureLocation) {
		Object templateModel = FreemarkerUtils.templateModelForObject(featureLocation);
		return FreemarkerUtils.processTemplate(valueTemplate, templateModel);
	}
	
}
