package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider;

import org.w3c.dom.Element;

import freemarker.template.Template;
import freemarker.template.TemplateModel;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;

@PluginClass(elemName="freemarkerSourceInfoProvider")
public class FreemarkerSourceInfoProvider extends SourceInfoProvider {

	private Template template;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.template = PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, "template", true);
	}

	@Override
	public String provideSourceInfo(Sequence sequence) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(sequence);
		return FreemarkerUtils.processTemplate(template, templateModel);
	}
	
}
