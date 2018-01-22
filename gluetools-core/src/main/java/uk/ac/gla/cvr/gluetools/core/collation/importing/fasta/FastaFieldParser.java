/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class FastaFieldParser implements Plugin, PropertyPopulator {

	private String fieldName;
	private Pattern nullRegex = null;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters = null;
	private Boolean overwriteExistingNonNull;
	private Boolean overwriteWithNewNull;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
		nullRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
				orElse(Pattern.compile(PropertyPopulator.DEFAULT_NULL_REGEX));
		overwriteExistingNonNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteExistingNonNull", false)).orElse(false);
		overwriteWithNewNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteWithNewNull", false)).orElse(false);
		valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter"));
		mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem);
	}

	public Optional<Result> parseField(String inputText) {
		String fieldValue = SequencePopulator.runPropertyPopulator(this, inputText);
		if(fieldValue != null) {
			return Optional.of(new Result(this, fieldValue));
		} else {
			return Optional.empty();
		}
	}
	
	public class Result {
		private PropertyPopulator fieldPopulator;
		private String fieldValue;

		public Result(PropertyPopulator fieldPopulator, String fieldValue) {
			super();
			this.fieldPopulator = fieldPopulator;
			this.fieldValue = fieldValue;
		}
		
		public PropertyPopulator getFieldPopulator() {
			return fieldPopulator;
		}
		public String getFieldValue() {
			return fieldValue;
		}
	}

	@Override
	public String getProperty() {
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

	@Override
	public boolean overwriteExistingNonNull() {
		return overwriteExistingNonNull;
	}

	@Override
	public boolean overwriteWithNewNull() {
		return overwriteWithNewNull;
	}

}
