package uk.ac.gla.cvr.gluetools.core.collation.exporting.fastaProteinExporter;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.AbstractFastaExporter;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

@PluginClass(elemName="fastaProteinExporter", 
description="Exports amino acid data from a set of ReferenceSequence objects to a FASTA file")
public class FastaProteinExporter extends AbstractFastaExporter<FastaProteinExporter> {

	public static final String ID_TEMPLATE = "idTemplate";

	private Template idTemplate;

	public static final String DEFAULT_ID_TEMPLATE = "${name}.${sequence.source.name}.${sequence.sequenceID}";
	
	public FastaProteinExporter() {
		super();
		registerModulePluginCmdClass(ExportCommand.class);
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

	public String generateFastaId(ReferenceSequence refSeq) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(refSeq);
		return FreemarkerUtils.processTemplate(idTemplate, templateModel);
	}
	
	
	

}
