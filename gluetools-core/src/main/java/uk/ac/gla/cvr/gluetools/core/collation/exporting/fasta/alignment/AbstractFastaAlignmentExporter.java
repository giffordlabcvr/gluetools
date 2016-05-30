package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporterException;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.FastaExporterException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

public class AbstractFastaAlignmentExporter<T extends AbstractFastaAlignmentExporter<T>> extends ModulePlugin<T> {

	private static final String ID_TEMPLATE = "idTemplate";
	private static final String DEDUPLICATE = "deduplicate";
	private Template idTemplate;
	private Boolean deduplicate;
	
	public AbstractFastaAlignmentExporter() {
		super();
		addSimplePropertyName(ID_TEMPLATE);
		addSimplePropertyName(DEDUPLICATE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		idTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, ID_TEMPLATE, false))
				.orElse(FreemarkerUtils.templateFromString("${alignment.name}.${sequence.source.name}.${sequence.sequenceID}", pluginConfigContext.getFreemarkerConfiguration()));
		deduplicate = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DEDUPLICATE, false)).orElse(false);
	}

	protected String generateFastaId(AlignmentMember almtMember) {
		TemplateModel templateModel = FreemarkerUtils.templateModelForGlueDataObject(almtMember);
		return FreemarkerUtils.processTemplate(idTemplate, templateModel);
	}
	
	protected CommandResult formResult(ConsoleCommandContext cmdContext,
			String fastaString, String fileName, Boolean preview) {
		if(preview) {
			return new SimpleConsoleCommandResult(fastaString, false);
		} else {
			byte[] bytes = fastaString.getBytes();
			cmdContext.saveBytes(fileName, bytes);
			return new OkResult();
		}
	}

	protected void checkAlignment(Alignment alignment, String featureName, Boolean recursive) {
		ReferenceSequence refSequence = alignment.getRefSequence();
		if(refSequence == null && recursive) {
			throw new FastaExporterException(Code.CANNOT_SPECIFY_RECURSIVE_FOR_UNCONSTRAINED_ALIGNMENT, alignment.getName());
		}
		if(refSequence == null && featureName != null) {
			throw new FastaExporterException(Code.CANNOT_SPECIFY_FEATURE_FOR_UNCONSTRAINED_ALIGNMENT, alignment.getName());
		}
	}

	protected Boolean getDeduplicate() {
		return deduplicate;
	}


}
