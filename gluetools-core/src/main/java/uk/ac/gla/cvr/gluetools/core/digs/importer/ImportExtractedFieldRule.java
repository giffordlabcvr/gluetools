package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.Extracted;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName=ImportExtractedFieldRule.EXTRACTED_FIELD_RULE)
public class ImportExtractedFieldRule implements Plugin {

	public static final String EXTRACTED_FIELD_RULE = "extractedFieldRule";
	
	public static final String EXTRACTED_FIELD = "extractedField";
	public static final String SEQUENCE_FIELD = "sequenceField";
	public static final String FIELD_MISSING_ACTION = "fieldMissingAction";

	
	public enum FieldMissingAction {
		WARN, 
		IGNORE, 
		ERROR
	}

	private String extractedField;
	private String sequenceField;
	private FieldMissingAction fieldMissingAction = FieldMissingAction.WARN;

	
	public void setExtractedField(String extractedField) {
		this.extractedField = extractedField;
	}

	public String getExtractedField() {
		return extractedField;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		extractedField = PluginUtils.configureStringProperty(configElem, EXTRACTED_FIELD, true);
		List<String> extractedFieldOptions = Arrays.asList(Extracted.ALL_PROPERTIES);
		if(!extractedFieldOptions.contains(extractedField)) {
			throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, EXTRACTED_FIELD, "Field options: "+extractedFieldOptions, extractedField);
		}
		sequenceField = PluginUtils.configureStringProperty(configElem, SEQUENCE_FIELD, false);
		fieldMissingAction = Optional.of(PluginUtils
				.configureEnumProperty(FieldMissingAction.class, configElem, FIELD_MISSING_ACTION, false)).orElse(FieldMissingAction.WARN);
		
	}
	
	public void updateSequence(CommandContext cmdContext, Extracted extracted, Sequence sequence, String sequenceFieldToUse) {
		sequence.writeProperty(sequenceFieldToUse, extracted.readProperty(extractedField));
	}

	public String getSequenceFieldToUse() {
		String sequenceFieldToUse = sequenceField;
		if(sequenceFieldToUse == null) {
			sequenceFieldToUse = extractedFieldToSequenceField(extractedField);
		}
		return sequenceFieldToUse;
	}
	
	
	public static String extractedFieldToSequenceField(String extractedField) {
		StringBuffer sequenceField = new StringBuffer();
		sequenceField.append("DIGS_");
		if(extractedField.contains("_")) {
			sequenceField.append(extractedField.toUpperCase());
		} else {
			for(int i = 0; i < extractedField.length(); i++) {
				char c = extractedField.charAt(i);
				if(i > 0 && Character.isUpperCase(c) 
						&& Character.isLowerCase(extractedField.charAt(i-1))) {
					sequenceField.append("_");
				}
				sequenceField.append(Character.toUpperCase(c));
			}
		}
		return sequenceField.toString();
	}

	public FieldMissingAction getFieldMissingAction() {
		return fieldMissingAction;
	}

	
}
