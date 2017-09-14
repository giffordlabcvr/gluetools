package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

public class AbstractFastaAlignmentExporter<T extends AbstractFastaAlignmentExporter<T>> extends ModulePlugin<T> {

	public static final String ID_TEMPLATE = "idTemplate";

	private Template idTemplate;

	public static final String DEFAULT_ID_TEMPLATE = "${alignment.name}.${sequence.source.name}.${sequence.sequenceID}";

	public AbstractFastaAlignmentExporter() {
		super();
		addSimplePropertyName(ID_TEMPLATE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		idTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, ID_TEMPLATE, false))
				.orElse(FreemarkerUtils.templateFromString(DEFAULT_ID_TEMPLATE, pluginConfigContext.getFreemarkerConfiguration()));
	}

	public static String generateFastaId(Template idTemplate, AlignmentMember almtMember) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(almtMember);
		return FreemarkerUtils.processTemplate(idTemplate, templateModel);
	}
	
	public Template getIdTemplate() {
		return idTemplate;
	}
}
