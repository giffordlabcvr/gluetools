package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.populating.PropertyPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator.PropertyUpdate;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class BaseXmlPropertyPopulatorRule extends XmlPopulatorRule implements Plugin, PropertyPopulator {
		
		private String property;
		private Pattern nullRegex;
		private RegexExtractorFormatter mainExtractor = null;
		private List<RegexExtractorFormatter> valueConverters;
		private Boolean overwriteExistingNonNull;
		private Boolean overwriteWithNewNull;

		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
			overwriteExistingNonNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteExistingNonNull", false)).orElse(false);
			overwriteWithNewNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteWithNewNull", false)).orElse(false);
			nullRegex = Optional.ofNullable(
					PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
					orElse(Pattern.compile(DEFAULT_NULL_REGEX));
		}
		
		protected void setProperty(String property) {
			this.property = property;
		}
		
		protected void setMainExtractor(RegexExtractorFormatter mainExtractor) {
			this.mainExtractor = mainExtractor;
		}

		protected void setValueConverters(List<RegexExtractorFormatter> valueConverters) {
			this.valueConverters = valueConverters;
		}

		public void execute(XmlPopulatorContext xmlPopulatorContext, Node node) {
			if(!(xmlPopulatorContext.isAllowedField(property) || xmlPopulatorContext.isAllowedLink(property))) {
				return;
			}
			if(xmlPopulatorContext.getPropertyUpdates().containsKey(property)) {
				return; // we already have an update for this field.
			}
			String selectedText;
			try {
				selectedText = GlueXmlUtils.getNodeText(node);
			} catch (Exception e) {
				throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
			}
			if(selectedText != null) {
				String propertyPopulatorResult = SequencePopulator.runPropertyPopulator(this, selectedText);
				if(propertyPopulatorResult != null) {
					FieldType fieldType = xmlPopulatorContext.getFieldType(property);
					String customTableName = xmlPopulatorContext.getCustomTableName(property);
					PropertyUpdate propertyUpdate = SequencePopulator
							.generatePropertyUpdate(fieldType, customTableName, xmlPopulatorContext.getSequence(), this, propertyPopulatorResult);
					if(propertyUpdate != null && propertyUpdate.updated()) {
						xmlPopulatorContext.getPropertyUpdates().put(propertyUpdate.getProperty(), propertyUpdate);
					}
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
		public void validate(CommandContext cmdContext) {
			Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
			project.checkModifiableProperties(ConfigurableTable.sequence.name(), Arrays.asList(property));
			
		}
		
		
	
}