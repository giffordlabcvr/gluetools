package uk.ac.gla.cvr.gluetools.core.collation.exporting.fastaProteinExporter;

import java.util.Optional;

import org.w3c.dom.Element;

import freemarker.template.Template;
import freemarker.template.TemplateModel;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.AbstractFastaExporter;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;

@PluginClass(elemName="fastaProteinExporter", 
description="Exports amino acid data from ReferenceSequence or AlignmentMember objects to a FASTA file")
public class FastaProteinExporter extends AbstractFastaExporter<FastaProteinExporter> {

	public static final String REF_ID_TEMPLATE = "refIdTemplate";
	public static final String MEMBER_ID_TEMPLATE = "memberIdTemplate";

	private Template refIdTemplate;
	private Template memberIdTemplate;

	public static final String DEFAULT_REF_ID_TEMPLATE = "${name}.${sequence.source.name}.${sequence.sequenceID}";
	public static final String DEFAULT_MEMBER_ID_TEMPLATE = "${alignment.name}.${sequence.source.name}.${sequence.sequenceID}";
	
	public FastaProteinExporter() {
		super();
		registerModulePluginCmdClass(ExportReferenceCommand.class);
		registerModulePluginCmdClass(ExportMemberCommand.class);
		addSimplePropertyName(REF_ID_TEMPLATE);
		addSimplePropertyName(MEMBER_ID_TEMPLATE);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refIdTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, REF_ID_TEMPLATE, false))
				.orElse(FreemarkerUtils.templateFromString(DEFAULT_REF_ID_TEMPLATE, pluginConfigContext.getFreemarkerConfiguration()));
		memberIdTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, MEMBER_ID_TEMPLATE, false))
				.orElse(FreemarkerUtils.templateFromString(DEFAULT_MEMBER_ID_TEMPLATE, pluginConfigContext.getFreemarkerConfiguration()));
	}

	public String generateReferenceFastaId(ReferenceSequence refSeq) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(refSeq);
		return FreemarkerUtils.processTemplate(refIdTemplate, templateModel);
	}

	public String generateMemberFastaId(AlignmentMember almtMember) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForObject(almtMember);
		return FreemarkerUtils.processTemplate(memberIdTemplate, templateModel);
	}
	
	
	

}
