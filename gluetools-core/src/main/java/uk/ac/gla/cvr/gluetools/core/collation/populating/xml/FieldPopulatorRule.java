package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUsage;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

@PluginClass(elemName="fieldPopulator")
public class FieldPopulatorRule extends XmlPopulatorRule implements Plugin {

	
	private String dataFieldName;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		dataFieldName = PluginUtils.configureString(configElem, "@fieldName", true);
		valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter"));
		mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem);
	}

	
	private String extractAndConvert(String input) {
		if(mainExtractor != null) {
			String mainExtractorResult = mainExtractor.matchAndConvert(input);
			if(mainExtractorResult == null) {
				return null;
			} else {
				input = mainExtractorResult;
			}
		}
		for(RegexExtractorFormatter valueConverter: valueConverters) {
			String valueConverterResult = valueConverter.matchAndConvert(input);
			if(valueConverterResult != null) {
				return valueConverterResult;
			}
		}
		return input;
	}
	
	private String getDataFieldName() {
		return dataFieldName;
	}

	
	
	public void execute(CommandContext cmdContext, Node node) {
		String dataFieldName = getDataFieldName();
		String selectedText;
		try {
			selectedText = XmlUtils.getNodeText(node);
		} catch (Exception e) {
			throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
		}
		if(selectedText != null) {
			String extractAndConvertResult = extractAndConvert(selectedText);
			if(extractAndConvertResult != null) {
				Element setFieldElem = CommandUsage.docElemForCmdClass(SetFieldCommand.class);
				XmlUtils.appendElementWithText(setFieldElem, SetFieldCommand.FIELD_NAME, dataFieldName);
				XmlUtils.appendElementWithText(setFieldElem, SetFieldCommand.FIELD_VALUE, extractAndConvertResult);
				XmlUtils.appendElementWithText(setFieldElem, SetFieldCommand.NO_OVERWRITE, "true");
				cmdContext.executeElem(setFieldElem.getOwnerDocument().getDocumentElement());
			}
		}
	}
	
}
