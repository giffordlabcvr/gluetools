package uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator;

import java.util.Optional;

import org.w3c.dom.Element;

import freemarker.template.Configuration;
import freemarker.template.Template;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;

@PluginClass(elemName="freemarkerAnnotationGenerator")
public class FreemarkerAnnotationGenerator extends MemberAnnotationGenerator {

	public static final String TEMPLATE = "template";
	
	private Template template;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.template = 
				Optional.ofNullable(PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, TEMPLATE, false))
						.orElse(defaultTemplate(pluginConfigContext.getFreemarkerConfiguration()));
	}

	private Template defaultTemplate(Configuration freemarkerConfiguration) {
		return FreemarkerUtils.templateFromString("${renderNestedProperty('"+getAnnotationName()+"')}", freemarkerConfiguration);
	}

	@Override
	public String renderAnnotation(CommandContext cmdContext, AlignmentMember member) {
		return FreemarkerUtils.processTemplate(template, FreemarkerUtils.templateModelForObject(member));
	}
}
