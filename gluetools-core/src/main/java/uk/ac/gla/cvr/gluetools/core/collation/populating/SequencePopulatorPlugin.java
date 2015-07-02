package uk.ac.gla.cvr.gluetools.core.collation.populating;

import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public abstract class SequencePopulatorPlugin<P extends ModulePlugin<P>> extends ModulePlugin<P> {

	public static final String WHERE_CLAUSE = "whereClause";
	private Optional<Expression> whereClause;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
	}
	
	protected Optional<Expression> getWhereClause() {
		return whereClause;
	}

	
	public static void populateField(CommandContext cmdContext, FieldPopulator fieldPopulator, String inputText) {
		String extractAndConvertResult = 
				RegexExtractorFormatter.extractAndConvert(inputText, fieldPopulator.getMainExtractor(), fieldPopulator.getValueConverters());
		if(extractAndConvertResult != null) {
			if(!fieldPopulator.getNullRegex().matcher(extractAndConvertResult).find()) {
				Element setFieldElem = CommandUsage.docElemForCmdClass(SetFieldCommand.class);
				XmlUtils.appendElementWithText(setFieldElem, SetFieldCommand.FIELD_NAME, fieldPopulator.getFieldName());
				XmlUtils.appendElementWithText(setFieldElem, SetFieldCommand.FIELD_VALUE, extractAndConvertResult);
				XmlUtils.appendElementWithText(setFieldElem, SetFieldCommand.NO_OVERWRITE, "true");
				cmdContext.executeElem(setFieldElem.getOwnerDocument().getDocumentElement());
			}
		}

	}
}
