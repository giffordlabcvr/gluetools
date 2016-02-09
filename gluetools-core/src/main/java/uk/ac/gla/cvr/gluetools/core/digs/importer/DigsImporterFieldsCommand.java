package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedSchemaProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table.ListFieldCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.digs.importer.model.Extracted;

public abstract class DigsImporterFieldsCommand<R extends CommandResult> extends ModulePluginCommand<R, DigsImporter> implements ProvidedSchemaProjectModeCommand  {

	protected static Map<String, GlueFieldInfo> extractedToGlueFieldInfo = new LinkedHashMap<String, GlueFieldInfo>();
	
	static {
		extractedToGlueFieldInfo.put(Extracted.ALIGN_LEN_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.ASSIGNED_GENE_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.ASSIGNED_NAME_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.BLAST_ID_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.BIT_SCORE_PROPERTY, new GlueFieldInfo("DOUBLE"));
		extractedToGlueFieldInfo.put(Extracted.DATA_TYPE_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.EXTRACT_END_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.EXTRACT_START_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.GAP_OPENINGS_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.IDENTITY_PROPERTY, new GlueFieldInfo("DOUBLE"));
		extractedToGlueFieldInfo.put(Extracted.MISMATCHES_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.ORGANISM_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.ORIENTATION_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.PROBE_TYPE_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.QUERY_END_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.QUERY_START_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.SCAFFOLD_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.SUBJECT_END_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.SUBJECT_START_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.TARGET_NAME_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.VERSION_PROPERTY, new GlueFieldInfo("VARCHAR", 100));
		extractedToGlueFieldInfo.put(Extracted.E_VALUE_EXP_PROPERTY, new GlueFieldInfo("INTEGER"));
		extractedToGlueFieldInfo.put(Extracted.E_VALUE_NUM_PROPERTY, new GlueFieldInfo("DOUBLE"));
	}
	
	protected static class GlueFieldInfo {
		public String glueType;
		public Integer glueVarcharLength = null;

		public GlueFieldInfo(String glueType, Integer glueVarcharLength) {
			super();
			this.glueType = glueType;
			this.glueVarcharLength = glueVarcharLength;
		}

		public GlueFieldInfo(String glueType) {
			super();
			this.glueType = glueType;
		}
		
	}
	
	protected static void checkForFieldConflicts(String extractedField,
			String sequenceFieldToUse, Map<String, Object> actualGlueField,
			String requiredType, Integer requiredLength) {
		String actualType = (String) actualGlueField.get(Field.TYPE_PROPERTY);
		if(!requiredType.equals(actualType)) {
			throw new DigsImporterException(DigsImporterException.Code.SEQUENCE_FIELD_WRONG_TYPE,
					extractedField, sequenceFieldToUse, actualType, requiredType);
		}
		if(actualType.equals("VARCHAR")) {
			Integer actualLength = (Integer) actualGlueField.get(Field.MAX_LENGTH_PROPERTY);
			if(requiredLength > actualLength) {
				throw new DigsImporterException(DigsImporterException.Code.SEQUENCE_FIELD_INSUFFICIENT_LENGTH,
						extractedField, sequenceFieldToUse, actualLength, requiredLength);
			}
		}
	}

	protected static Map<String, Map<String, Object>> getExistingFieldProperties(
			CommandContext cmdContext) {
		ListResult listSeqFieldsResult = cmdContext.cmdBuilder(ListFieldCommand.class).build().execute(cmdContext);
		Map<String, Map<String, Object>> glueSeqFieldNameToFieldProperties = new LinkedHashMap<String, Map<String,Object>>();
		for(Map<String, Object> listSeqFieldsRow: listSeqFieldsResult.asListOfMaps()) {
			glueSeqFieldNameToFieldProperties.put((String) listSeqFieldsRow.get(Field.NAME_PROPERTY), listSeqFieldsRow);
		}
		return glueSeqFieldNameToFieldProperties;
	}


	
}
