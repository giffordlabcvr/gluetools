package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseTextFilePopulatorColumn implements Plugin, PropertyPopulator {

	private Optional<Boolean> identifier;
	private Optional<String> header;
	private Optional<Integer> number;
	private String property;
	private Pattern nullRegex;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters;
	private Boolean overwriteExistingNonNull;
	private Boolean overwriteWithNewNull;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		identifier = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "identifier", false));
		header = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, "header", false));
		nullRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
				orElse(Pattern.compile(DEFAULT_NULL_REGEX));
		property = PluginUtils.configureStringProperty(configElem, "property", false);
		if(property == null) {
			property = PluginUtils.configureStringProperty(configElem, "fieldName", false);
			GlueLogger.getGlueLogger().warning("<fieldName> property of <"+configElem.getNodeName()+"> is deprecated, please use <property> instead.");
		}
		if(property == null) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "Property must be defined.");
		}
		number = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "number", false));
		if(header.isPresent() && number.isPresent()) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "At most one of header and number may be defined.");
		}
		if(!header.isPresent() && !number.isPresent()) {
			header = Optional.of(property);
		}
		overwriteExistingNonNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteExistingNonNull", false)).orElse(false);
		overwriteWithNewNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteWithNewNull", false)).orElse(false);
	}

	public Optional<Boolean> getIdentifier() {
		return identifier;
	}

	public String getProperty() {
		return property;
	}

	public Optional<String> getHeader() {
		return header;
	}

	public Optional<Integer> getNumber() {
		return number;
	}

	public RegexExtractorFormatter getMainExtractor() {
		return mainExtractor;
	}

	public List<RegexExtractorFormatter> getValueConverters() {
		return valueConverters;
	}
	
	public void setMainExtractor(RegexExtractorFormatter mainExtractor) {
		this.mainExtractor = mainExtractor;
	}

	public void setValueConverters(List<RegexExtractorFormatter> valueConverters) {
		this.valueConverters = valueConverters;
	}

	public Pattern getNullRegex() {
		return nullRegex;
	}
	
	@Override
	public boolean overwriteExistingNonNull() {
		return overwriteExistingNonNull;
	}

	@Override
	public boolean overwriteWithNewNull() {
		return overwriteWithNewNull;
	}
}
