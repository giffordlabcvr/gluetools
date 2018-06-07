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
package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.populating.ValueExtractor;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.propertyPopulator.SequencePopulator.PropertyUpdate;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.MatcherConverter;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class BaseXmlPropertyPopulatorRule extends XmlPopulatorRule implements Plugin, ValueExtractor, PropertyPopulator {
		
		private String property;
		private Pattern nullRegex;
		private MatcherConverter mainExtractor = null;
		private List<? extends MatcherConverter> valueConverters;
		private Boolean overwriteExistingNonNull;
		private Boolean overwriteWithNewNull;
		private TraversedLinkStrategy traversedLinkStrategy;

		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
			overwriteExistingNonNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteExistingNonNull", false)).orElse(false);
			overwriteWithNewNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteWithNewNull", false)).orElse(false);
			nullRegex = Optional.ofNullable(
					PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
					orElse(Pattern.compile(DEFAULT_NULL_REGEX));
			traversedLinkStrategy = PluginUtils.configureEnumProperty(TraversedLinkStrategy.class, configElem, 
					"traversedLinkStrategy", TraversedLinkStrategy.SKIP_MISSING);

		}
		
		protected void setProperty(String property) {
			this.property = property;
		}
		
		protected void setMainExtractor(RegexExtractorFormatter mainExtractor) {
			this.mainExtractor = mainExtractor;
		}

		protected void setValueConverters(List<? extends MatcherConverter> valueConverters) {
			this.valueConverters = valueConverters;
		}

		public void execute(XmlPopulatorContext xmlPopulatorContext, Node node) {
			if(!(xmlPopulatorContext instanceof XmlPopulatorPropertyUpdateContext)) {
				return;
			}
			XmlPopulatorPropertyUpdateContext xmlPopulatorPropertyUpdateContext = 
					((XmlPopulatorPropertyUpdateContext) xmlPopulatorContext);
			
			
			PropertyPathInfo propertyPathInfo = xmlPopulatorPropertyUpdateContext.getPropertyPathInfo(property);
			if(propertyPathInfo == null) {
				return;
			}
			if(xmlPopulatorPropertyUpdateContext.getPropertyUpdates().containsKey(property)) {
				return; // we already have an update for this field.
			}
			String selectedText;
			try {
				selectedText = GlueXmlUtils.getNodeText(node);
			} catch (Exception e) {
				throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
			}
			if(selectedText != null) {
				String valueExtractorResult = ValueExtractor.extractValue(this, selectedText);
				if(valueExtractorResult != null) {
					PropertyUpdate propertyUpdate = PropertyPopulator
							.generatePropertyUpdate(propertyPathInfo, xmlPopulatorContext.getSequence(), this, valueExtractorResult);
					if(propertyUpdate.updated()) {
						xmlPopulatorPropertyUpdateContext.getPropertyUpdates().put(propertyPathInfo.getPropertyPath(), propertyUpdate);
					}
				}
			}
		}

		@Override
		public MatcherConverter getMainExtractor() {
			return mainExtractor;
		}

		@Override
		public List<? extends MatcherConverter> getValueConverters() {
			return valueConverters;
		}

		@Override
		public Pattern getNullRegex() {
			return nullRegex;
		}

		@Override
		public String getProperty() {
			return property;
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

		@Override
		public void validate(CommandContext cmdContext) {
			Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
			PropertyPopulator.analysePropertyPath(project, ConfigurableTable.sequence.name(), property);
		}

		@Override
		public List<String> updatablePropertyPaths() {
			return Arrays.asList(property);
		}
		
		
	
}
