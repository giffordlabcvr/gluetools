package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.populating.FieldPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="fieldPopulator")
public class XmlFieldPopulatorRule extends XmlPopulatorRule implements Plugin, FieldPopulator {

	
	private String fieldName;
	private Pattern nullRegex;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters;
	private Boolean overwrite;
	private Boolean forceUpdate;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		fieldName = PluginUtils.configureString(configElem, "@fieldName", true);
		overwrite = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwrite", false)).orElse(false);
		forceUpdate = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "forceUpdate", false)).orElse(false);
		nullRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
				orElse(Pattern.compile(DEFAULT_NULL_REGEX));
		valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter"));
		mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem);
	}
	
	public void execute(XmlPopulatorContext xmlPopulatorContext, Node node) {
		if(!xmlPopulatorContext.isAllowedField(fieldName)) {
			return;
		}
		String selectedText;
		try {
			selectedText = GlueXmlUtils.getNodeText(node);
		} catch (Exception e) {
			throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
		}
		if(selectedText != null) {
			SequencePopulator.FieldUpdateResult fieldUpdateResult = SequencePopulator.populateField(xmlPopulatorContext.getCmdContext(), this, selectedText);
			if(fieldUpdateResult != null && fieldUpdateResult.updated()) {
				xmlPopulatorContext.getFieldUpdates().put(fieldUpdateResult.getFieldName(), fieldUpdateResult.getFieldValue());
			}
		}
	}

	@Override
	public RegexExtractorFormatter getMainExtractor() {
		return mainExtractor;
	}

	@Override
	public List<RegexExtractorFormatter> getValueConverters() {
		return valueConverters;
	}

	@Override
	public Pattern getNullRegex() {
		return nullRegex;
	}

	@Override
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public boolean getOverwrite() {
		return overwrite;
	}

	@Override
	public boolean getForceUpdate() {
		return forceUpdate;
	}
}
