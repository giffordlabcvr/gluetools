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

import uk.ac.gla.cvr.gluetools.core.collation.populating.ValueExtractor;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class FastaFieldParser implements Plugin, ValueExtractor, PropertyPopulator {

	private String property;
	private Pattern nullRegex = null;
	private RegexExtractorFormatter mainExtractor = null;
	private List<RegexExtractorFormatter> valueConverters = null;
	private Boolean overwriteExistingNonNull;
	private Boolean overwriteWithNewNull;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		property = PluginUtils.configureStringProperty(configElem, "property", false);
		String fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", false);
		if(fieldName != null) {
			GlueLogger.getGlueLogger().warning("Element <fieldName> is deprecated, use element property instead");
		}
		if(property == null && fieldName == null) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "No target property defined");
		}
		if(fieldName != null && this.property == null) {
			this.property = fieldName;
		}
		nullRegex = Optional.ofNullable(
				PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
				orElse(Pattern.compile(ValueExtractor.DEFAULT_NULL_REGEX));
		overwriteExistingNonNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteExistingNonNull", false)).orElse(false);
		overwriteWithNewNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteWithNewNull", false)).orElse(false);
		valueConverters = PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter"));
		mainExtractor = PluginFactory.createPlugin(pluginConfigContext, RegexExtractorFormatter.class, configElem);
	}

	public Optional<Result> parseField(String inputText) {
		String fieldValue = ValueExtractor.extractValue(this, inputText);
		if(fieldValue != null) {
			return Optional.of(new Result(this, fieldValue));
		} else {
			return Optional.empty();
		}
	}
	
	public class Result {
		private PropertyPopulator propertyPopulator;
		private String value;

		public Result(PropertyPopulator propertyPopulator, String value) {
			super();
			this.propertyPopulator = propertyPopulator;
			this.value = value;
		}
		
		public PropertyPopulator getPropertyPopulator() {
			return propertyPopulator;
		}
		public String getValue() {
			return value;
		}
	}

	@Override
	public String getProperty() {
		return property;
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
