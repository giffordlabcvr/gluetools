package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.populating.FieldPopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator;
import uk.ac.gla.cvr.gluetools.core.collation.populating.SequencePopulator.FieldUpdate;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class BaseXmlFieldPopulatorRule extends XmlPopulatorRule implements Plugin, FieldPopulator {
		
		private String fieldName;
		private Pattern nullRegex;
		private RegexExtractorFormatter mainExtractor = null;
		private List<RegexExtractorFormatter> valueConverters;
		private Boolean overwriteExistingNonNull;
		private Boolean overwriteWithNewNull;

		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
			fieldName = PluginUtils.configureString(configElem, "@fieldName", true);
			overwriteExistingNonNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteExistingNonNull", false)).orElse(false);
			overwriteWithNewNull = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "overwriteWithNewNull", false)).orElse(false);
			nullRegex = Optional.ofNullable(
					PluginUtils.configureRegexPatternProperty(configElem, "nullRegex", false)).
					orElse(Pattern.compile(DEFAULT_NULL_REGEX));
		}
		
		protected void setMainExtractor(RegexExtractorFormatter mainExtractor) {
			this.mainExtractor = mainExtractor;
		}

		protected void setValueConverters(List<RegexExtractorFormatter> valueConverters) {
			this.valueConverters = valueConverters;
		}

		public void execute(XmlPopulatorContext xmlPopulatorContext, Node node) {
			if(!xmlPopulatorContext.isAllowedField(fieldName)) {
				return;
			}
			if(xmlPopulatorContext.getFieldUpdates().containsKey(fieldName)) {
				return; // we already have an update for this field.
			}
			String selectedText;
			try {
				selectedText = GlueXmlUtils.getNodeText(node);
			} catch (Exception e) {
				throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
			}
			if(selectedText != null) {
				String fieldPopulatorResult = SequencePopulator.runFieldPopulator(this, selectedText);
				if(fieldPopulatorResult != null) {
					FieldUpdate fieldUpdateResult = SequencePopulator
							.generateFieldUpdate(xmlPopulatorContext.getFieldType(fieldName), xmlPopulatorContext.getSequence(), this, fieldPopulatorResult);
					if(fieldUpdateResult != null && fieldUpdateResult.updated()) {
						xmlPopulatorContext.getFieldUpdates().put(fieldUpdateResult.getFieldName(), fieldUpdateResult);
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
		public String getFieldName() {
			return fieldName;
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
			project.checkModifiableFieldNames(ConfigurableTable.sequence.name(), Arrays.asList(fieldName));
			
		}
		
		
	
}
