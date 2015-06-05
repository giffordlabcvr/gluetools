package uk.ac.gla.cvr.gluetools.core.datafield.populator.xml;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorException;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.PopulatorRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class FieldPopulatorRule extends PopulatorRule implements Plugin {

	public static String ELEM_NAME = "fieldPopulator";
	
	private String dataFieldName;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters;

	@Override
	public void configure(Element configElem)  {
		dataFieldName = PluginUtils.configureString(configElem, "@fieldName", true);
		mainExtractor = PluginFactory.createPlugin(RegexExtractorFormatter.class, configElem);
		valueConverters = PluginFactory.createPlugins(RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter"));
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

	
	public void execute(CollatedSequence collatedSequence, Node node) {
		String dataFieldName = getDataFieldName();
		if(!collatedSequence.getOwningProject().hasDataField(dataFieldName)) {
			return;
		}
		if(collatedSequence.hasDataFieldValue(dataFieldName)) {
			return;
		}
		String selectedText;
		try {
			selectedText = XmlUtils.getNodeText(node);
		} catch (Exception e) {
			throw new DataFieldPopulatorException(e, DataFieldPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
		}
		if(selectedText != null) {
			String extractAndConvertResult = extractAndConvert(selectedText);
			if(extractAndConvertResult != null) {
				collatedSequence.setDataFieldValue(dataFieldName, extractAndConvertResult);
			}
		}
	}
	
}
