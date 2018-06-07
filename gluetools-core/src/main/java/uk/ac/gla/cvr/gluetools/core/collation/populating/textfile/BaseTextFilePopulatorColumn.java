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
package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.ValueExtractor;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.MatcherConverter;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseTextFilePopulatorColumn implements Plugin, ValueExtractor, PropertyPopulator {

	private Optional<Boolean> identifier;
	private Optional<String> header;
	private Optional<Integer> number;
	private String property;
	private Pattern nullRegex;
	private MatcherConverter mainExtractor = null;
	private List<? extends MatcherConverter> valueConverters;
	private Boolean overwriteExistingNonNull;
	private Boolean overwriteWithNewNull;
	private TraversedLinkStrategy traversedLinkStrategy;

	
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
		traversedLinkStrategy = PluginUtils.configureEnumProperty(TraversedLinkStrategy.class, configElem, "traversedLinkStrategy", TraversedLinkStrategy.MUST_EXIST);
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

	public MatcherConverter getMainExtractor() {
		return mainExtractor;
	}

	public List<? extends MatcherConverter> getValueConverters() {
		return valueConverters;
	}
	
	public void setMainExtractor(MatcherConverter mainExtractor) {
		this.mainExtractor = mainExtractor;
	}

	public void setValueConverters(List<? extends MatcherConverter> valueConverters) {
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
	
	@Override
	public TraversedLinkStrategy getTraversedLinkStrategy() {
		return traversedLinkStrategy;
	}
}
