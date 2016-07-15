package uk.ac.gla.cvr.gluetools.core.reporting.figtree.annotationExporter;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class FigTreeAnnotation implements Plugin {

	public static final String ANNOTATION_NAME = "annotationName";
	public static final String VALUE_FREEMARKER_TEMPLATE = "valueFreemarkerTemplate";
	
	private String annotationName;
	private Template valueFreemarkerTemplate;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		this.annotationName = PluginUtils.configureStringProperty(configElem, ANNOTATION_NAME, true);
		this.valueFreemarkerTemplate = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, VALUE_FREEMARKER_TEMPLATE, false);
	}

	public String getAnnotationName() {
		return annotationName;
	}

	public void setAnnotationName(String annotationName) {
		this.annotationName = annotationName;
	}

	public Template getValueFreemarkerTemplate(Configuration freemarkerConfiguration) {
		if(this.valueFreemarkerTemplate == null) {
			return FreemarkerUtils.templateFromString("${renderNestedProperty('"+this.annotationName+"')}", 
					freemarkerConfiguration);
		}
		return valueFreemarkerTemplate;
	}
	
	
}
