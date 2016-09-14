package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

public class AbstractFastaExporter<T extends AbstractFastaExporter<T>> extends ModulePlugin<T> {

	private static final String ID_TEMPLATE = "idTemplate";
	private Template idTemplate;
	
	public static final String DEFAULT_ID_TEMPLATE = "${sequenceID}";

	
	public AbstractFastaExporter() {
		super();
		addSimplePropertyName(ID_TEMPLATE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.idTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, ID_TEMPLATE, false))
				.orElse(FreemarkerUtils.templateFromString(DEFAULT_ID_TEMPLATE, pluginConfigContext.getFreemarkerConfiguration()));
	}

	protected String generateFastaId(Sequence sequence) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForGlueDataObject(sequence);
		return FreemarkerUtils.processTemplate(idTemplate, templateModel);
	}

	

}
