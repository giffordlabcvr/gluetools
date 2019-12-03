package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.structuredCommentProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import freemarker.template.Template;
import freemarker.template.TemplateModel;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;

@PluginClass(elemName="structuredCommentProvider")
public class StructuredCommentProvider implements Plugin {

	private List<Template> sequencingTechnologyTemplates = new ArrayList<Template>();
	private List<Template> assemblyMethodTemplates = new ArrayList<Template>();

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.sequencingTechnologyTemplates = PluginUtils.configureFreemarkerTemplatesProperty(pluginConfigContext, configElem, "sequencingTechnology");
		this.assemblyMethodTemplates = PluginUtils.configureFreemarkerTemplatesProperty(pluginConfigContext, configElem, "assemblyMethod");
	}
	
	public Map<String, String> generateStructuredComments(Sequence sequence) {
		Map<String, String> structuredComments = new LinkedHashMap<String, String>();
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(sequence);
		List<String> assemblyMethods = new ArrayList<String>();

		for(Template assemblyMethodTemplate: assemblyMethodTemplates) {
			String templateResult = FreemarkerUtils.processTemplate(assemblyMethodTemplate, templateModel).trim();	
			if(templateResult.length() > 0) {
				assemblyMethods.add(templateResult);
			}
		}
		if(!assemblyMethods.isEmpty()) {
			structuredComments.put("Assembly Method", String.join("; ", assemblyMethods));
		}
		List<String> sequencingTechnologies = new ArrayList<String>();

		for(Template sequencingTechnologyTemplate: sequencingTechnologyTemplates) {
			String templateResult = FreemarkerUtils.processTemplate(sequencingTechnologyTemplate, templateModel).trim();	
			if(templateResult.length() > 0) {
				sequencingTechnologies.add(templateResult);
			}
		}
		if(!sequencingTechnologies.isEmpty()) {
			structuredComments.put("Sequencing Technology", String.join("; ", sequencingTechnologies));
		}
		return structuredComments;
	}
	
}