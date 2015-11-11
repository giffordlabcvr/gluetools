package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.FieldPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="textFileColumn")
public class TextFilePopulatorColumn implements Plugin, FieldPopulator {

	private Optional<Boolean> identifier;
	private Optional<String> header;
	private Optional<Integer> number;
	private String fieldName;
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
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
		number = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "number", false));
		if(header.isPresent() && number.isPresent()) {
			throw new PluginConfigException(Code.CONFIG_CONSTRAINT_VIOLATION, "At most one of header and number may be defined.");
		}
		if(!header.isPresent() && !number.isPresent()) {
			header = Optional.of(fieldName);
		}
		overwriteExistingNonNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteExistingNonNull", false)).orElse(false);
		overwriteWithNewNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteWithNewNull", false)).orElse(false);
		valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter"));
		mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem);
	}

	public Optional<Boolean> getIdentifier() {
		return identifier;
	}

	public String getFieldName() {
		return fieldName;
	}

	public Optional<String> getHeader() {
		return header;
	}

	public Optional<Integer> getNumber() {
		return number;
	}

	public void processCellText(TextFilePopulatorContext populatorContext, String cellText) {
		ConsoleCommandContext cmdContext = populatorContext.cmdContext;
		SequencePopulator.populateField(cmdContext, this, cellText);
	}

	public RegexExtractorFormatter getMainExtractor() {
		return mainExtractor;
	}

	public List<RegexExtractorFormatter> getValueConverters() {
		return valueConverters;
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
