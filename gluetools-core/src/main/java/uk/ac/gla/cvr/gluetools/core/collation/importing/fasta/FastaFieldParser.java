package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.FieldPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class FastaFieldParser implements Plugin, FieldPopulator {

	private String fieldName;
	private Pattern nullRegex = null;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters = null;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
		nullRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
				orElse(Pattern.compile(FieldPopulator.DEFAULT_NULL_REGEX));
		valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter"));
		mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem);
	}

	public Optional<Result> parseField(String inputText) {
		String fieldValue = SequencePopulator.runFieldPopulator(this, inputText);
		if(fieldValue != null) {
			return Optional.of(new Result(this, fieldValue));
		} else {
			return Optional.empty();
		}
	}
	
	public class Result {
		private FieldPopulator fieldPopulator;
		private String fieldValue;

		public Result(FieldPopulator fieldPopulator, String fieldValue) {
			super();
			this.fieldPopulator = fieldPopulator;
			this.fieldValue = fieldValue;
		}
		
		public FieldPopulator getFieldPopulator() {
			return fieldPopulator;
		}
		public String getFieldValue() {
			return fieldValue;
		}
	}

	@Override
	public String getFieldName() {
		return fieldName;
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
}
