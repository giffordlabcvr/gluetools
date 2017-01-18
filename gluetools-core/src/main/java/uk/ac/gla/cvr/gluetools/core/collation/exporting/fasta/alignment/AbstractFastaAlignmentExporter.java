package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FreemarkerUtils;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

public class AbstractFastaAlignmentExporter<T extends AbstractFastaAlignmentExporter<T>> extends ModulePlugin<T> {

	public static final String ID_TEMPLATE = "idTemplate";
	public static final String EXCLUDE_EMPTY_ROWS = "excludeEmptyRows";

	private Template idTemplate;
	private Boolean excludeEmptyRows;

	public static final String DEFAULT_ID_TEMPLATE = "${alignment.name}.${sequence.source.name}.${sequence.sequenceID}";

	public AbstractFastaAlignmentExporter() {
		super();
		addSimplePropertyName(ID_TEMPLATE);
		addSimplePropertyName(EXCLUDE_EMPTY_ROWS);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		idTemplate = Optional.ofNullable(
				PluginUtils.configureFreemarkerTemplateProperty(pluginConfigContext, configElem, ID_TEMPLATE, false))
				.orElse(FreemarkerUtils.templateFromString(DEFAULT_ID_TEMPLATE, pluginConfigContext.getFreemarkerConfiguration()));
		excludeEmptyRows = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, EXCLUDE_EMPTY_ROWS, false)).orElse(Boolean.FALSE);
	}

	protected static String generateFastaId(Template idTemplate, AlignmentMember almtMember) {
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

	protected Template getIdTemplate() {
		return idTemplate;
	}

	protected Boolean getExcludeEmptyRows() {
		return excludeEmptyRows;
	}
	
	
}
