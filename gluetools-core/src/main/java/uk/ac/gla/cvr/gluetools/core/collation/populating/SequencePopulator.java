package uk.ac.gla.cvr.gluetools.core.collation.populating;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectSetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectUnsetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceMode;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class SequencePopulator<P extends ModulePlugin<P>> extends ModulePlugin<P> {

	
	public static FieldUpdate populateField(CommandContext cmdContext, FieldPopulator fieldPopulator, String inputText) {
		String fieldPopulatorResult = runFieldPopulator(fieldPopulator, inputText);
		if(fieldPopulatorResult != null) {
			return runSetFieldCommand(cmdContext, fieldPopulator, fieldPopulatorResult, true);
		}
		return null;
	}

	public static String runFieldPopulator(FieldPopulator fieldPopulator, String inputText) {
		String extractAndConvertResult = 
				RegexExtractorFormatter.extractAndConvert(inputText, fieldPopulator.getMainExtractor(), fieldPopulator.getValueConverters());
		if(extractAndConvertResult != null) {
			Pattern nullRegex = fieldPopulator.getNullRegex();
			if(nullRegex == null || !nullRegex.matcher(extractAndConvertResult).matches()) {
				return extractAndConvertResult;
			}
		}
		return null;
	}

	public static FieldUpdate generateFieldUpdate(FieldType fieldType, Sequence sequence, FieldPopulator fieldPopulator, String fieldValue) {
		String fieldName = fieldPopulator.getFieldName();
		boolean overwriteExistingNonNull = fieldPopulator.overwriteExistingNonNull();
		boolean overwriteWithNewNull = fieldPopulator.overwriteWithNewNull();
		
		Object oldValue = sequence.readProperty(fieldName);
		if(!overwriteExistingNonNull) {
			if(oldValue != null) {
				return new FieldUpdate(false, fieldName, fieldValue);
			}
		}
		if(!overwriteWithNewNull && fieldValue == null) {
			return new FieldUpdate(false, fieldName, fieldValue);
		}
		String oldValueString = null;
		if(oldValue != null) {
			oldValueString = fieldType.getFieldTranslator().objectValueToString(oldValue);
		}
		if(equals(oldValueString, fieldValue)) {
			return new FieldUpdate(false, fieldName, fieldValue);
		} else {
			return new FieldUpdate(true, fieldName, fieldValue);
		}
	}

	private static boolean equals(String string1, String string2) {
		if(string1 == null && string2 == null) {
			return true;
		}
		if(string1 != null && string2 == null) {
			return false;
		}
		if(string2 != null && string1 == null) {
			return false;
		}
		return(string1.equals(string2));
	}
	
	
	public static FieldUpdate runSetFieldCommand(CommandContext cmdContext,
			FieldPopulator fieldPopulator, String fieldValue, boolean noCommit) {
		String fieldName = fieldPopulator.getFieldName();
		boolean overwriteExistingNonNull = fieldPopulator.overwriteExistingNonNull();
		boolean overwriteWithNewNull = fieldPopulator.overwriteWithNewNull();
		
		if(!overwriteExistingNonNull) {
			SequenceMode sequenceMode = SequenceModeCommand.getSequenceMode(cmdContext);
			Project project = sequenceMode.getProject();
			project.checkCustomFieldNames(ConfigurableTable.sequence.name(), Collections.singletonList(fieldName));
			Sequence sequence = GlueDataObject.lookup(cmdContext, Sequence.class, 
					Sequence.pkMap(sequenceMode.getSourceName(), sequenceMode.getSequenceID()));
			Object oldValue = sequence.readProperty(fieldName);
			if(oldValue != null) {
				return new FieldUpdate(false, fieldName, fieldValue);
			}
		}
		if(!overwriteWithNewNull && fieldValue == null) {
			return new FieldUpdate(false, fieldName, fieldValue);
		}
		UpdateResult updateResult;
		if(fieldValue == null) {
			updateResult = cmdContext.cmdBuilder(ConfigurableObjectUnsetFieldCommand.class)
					.set(ConfigurableObjectUnsetFieldCommand.FIELD_NAME, fieldName)
					.set(ConfigurableObjectUnsetFieldCommand.NO_COMMIT, noCommit)
					.execute();
		} else {
			updateResult = cmdContext.cmdBuilder(ConfigurableObjectSetFieldCommand.class)
					.set(ConfigurableObjectSetFieldCommand.FIELD_NAME, fieldName)
					.set(ConfigurableObjectSetFieldCommand.FIELD_VALUE, fieldValue)
					.set(ConfigurableObjectSetFieldCommand.NO_COMMIT, noCommit)
					.execute();
		}
		if(updateResult.getNumber() == 1) {
			return new FieldUpdate(true, fieldName, fieldValue);
		}
		return new FieldUpdate(false, fieldName, fieldValue);
	}
	
	public static class FieldUpdate {
		private boolean updated;
		private String fieldValue;
		private String fieldName;

		public FieldUpdate(boolean updated, String fieldName, String fieldValue) {
			super();
			this.updated = updated;
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
		}

		public boolean updated() {
			return updated;
		}

		public String getFieldValue() {
			return fieldValue;
		}

		public String getFieldName() {
			return fieldName;
		}
		
		
		
	}

	protected void applyUpdateToDB(CommandContext cmdContext, Map<String, FieldType> fieldTypes, Sequence seq,
			FieldUpdate update) {
				Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
				String valueString = update.getFieldValue();
				String fieldName = update.getFieldName();
				if(valueString == null) {
					PropertyCommandDelegate.executeUnsetField(cmdContext, project, ConfigurableTable.sequence.name(), seq, fieldName, true);
				} else {
					Object fieldValue = fieldTypes.get(fieldName).getFieldTranslator().valueFromString(valueString);
					PropertyCommandDelegate.executeSetField(cmdContext, project, ConfigurableTable.sequence.name(), seq, fieldName, fieldValue, true);
				}
			}

	protected Map<String, FieldType> getFieldTypes(CommandContext cmdContext, List<String> fieldNames) {
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		if(fieldNames == null) {
			fieldNames = project.getModifiableFieldNames(ConfigurableTable.sequence.name());
		}
		Map<String, FieldType> fieldTypes = new LinkedHashMap<String, FieldType>();
		for(String fieldName: fieldNames) {
			fieldTypes.put(fieldName, 
				project.getModifiableFieldType(ConfigurableTable.sequence.name(), fieldName));
		}
		return fieldTypes;
	}
	
}
