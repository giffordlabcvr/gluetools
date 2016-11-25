package uk.ac.gla.cvr.gluetools.core.collation.populating;

import java.util.Collections;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectSetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectUnsetFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceMode;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SequenceModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class SequencePopulator<P extends ModulePlugin<P>> extends ModulePlugin<P> {

	
	public static FieldUpdateResult populateField(CommandContext cmdContext, FieldPopulator fieldPopulator, String inputText) {
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

	public static FieldUpdateResult runSetFieldCommand(CommandContext cmdContext,
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
				return new FieldUpdateResult(false, fieldName, fieldValue);
			}
		}
		if(!overwriteWithNewNull && fieldValue == null) {
			return new FieldUpdateResult(false, fieldName, fieldValue);
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
			return new FieldUpdateResult(true, fieldName, fieldValue);
		}
		return new FieldUpdateResult(false, fieldName, fieldValue);
	}
	
	public static class FieldUpdateResult {
		private boolean updated;
		private String fieldValue;
		private String fieldName;

		public FieldUpdateResult(boolean updated, String fieldName, String fieldValue) {
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
	
}
