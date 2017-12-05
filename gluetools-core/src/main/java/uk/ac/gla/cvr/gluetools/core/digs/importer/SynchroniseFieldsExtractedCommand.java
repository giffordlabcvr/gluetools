package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table.CreateFieldCommand;
import uk.ac.gla.cvr.gluetools.core.digs.importer.ImportExtractedFieldRule.GlueFieldRequirement;

@CommandClass(
		commandWords={"synchronise", "fields", "extracted"}, 
		description = "Ensure the GLUE sequence table has fields for DIGS \"Extracted\" data", 
		docoptUsages = { "" },
		docoptOptions = {
		},
		metaTags = { CmdMeta.updatesDatabase }, 
		furtherHelp = ""
)
public class SynchroniseFieldsExtractedCommand extends DigsImporterFieldsCommand<SynchroniseFieldsExtractedResult> {

	@Override
	protected SynchroniseFieldsExtractedResult execute(CommandContext cmdContext, DigsImporter digsImporter) {
		List<ImportExtractedFieldRule> importExtractedFieldRules = digsImporter.getImportExtractedFieldRules();
		List<Map<String, Object>> rowData = new ArrayList<Map<String, Object>>();
		try(ModeCloser modeCloser = cmdContext.pushCommandMode("table", "sequence")) {
			Map<String, Map<String, Object>> glueSeqFieldNameToFieldProperties = getExistingFieldProperties(cmdContext);
			for(ImportExtractedFieldRule rule: importExtractedFieldRules) {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				String extractedField = rule.getExtractedField();
				row.put(SynchroniseFieldsExtractedResult.DIGS_FIELD, extractedField);
				if(rule.getGlueFieldRequirement() == GlueFieldRequirement.IGNORE) {
					row.put(SynchroniseFieldsExtractedResult.GLUE_FIELD, null);
					row.put(SynchroniseFieldsExtractedResult.GLUE_TYPE, null);
					row.put(SynchroniseFieldsExtractedResult.GLUE_LENGTH, null);
					row.put(SynchroniseFieldsExtractedResult.STATUS, "ignored");
				} else {
					String sequenceFieldToUse = rule.getSequenceFieldToUse();
					row.put(SynchroniseFieldsExtractedResult.GLUE_FIELD, sequenceFieldToUse);
					GlueFieldInfo requiredGlueField = extractedToGlueFieldInfo.get(extractedField);
					Map<String, Object> actualGlueField = glueSeqFieldNameToFieldProperties.get(sequenceFieldToUse);
					String requiredType = requiredGlueField.glueType;
					Integer requiredLength = requiredGlueField.glueVarcharLength;
					row.put(SynchroniseFieldsExtractedResult.GLUE_TYPE, requiredType);
					row.put(SynchroniseFieldsExtractedResult.GLUE_LENGTH, requiredLength);
					if(actualGlueField == null) {
						// create the field
						CommandBuilder<CreateResult, CreateFieldCommand> builder = cmdContext.cmdBuilder(CreateFieldCommand.class)
						.set(CreateFieldCommand.FIELD_NAME, sequenceFieldToUse)
						.set(CreateFieldCommand.TYPE, requiredType);
						if(requiredLength != null) {
							builder.set(CreateFieldCommand.MAX_LENGTH, requiredLength);
						}
						builder.execute();
						row.put(SynchroniseFieldsExtractedResult.STATUS, "created");
					} else {
						checkForFieldConflicts(extractedField, sequenceFieldToUse, actualGlueField, requiredType, requiredLength);
						row.put(SynchroniseFieldsExtractedResult.STATUS, "exists");
					}
				}
				rowData.add(row);
			}
		}
		
		return new SynchroniseFieldsExtractedResult(rowData);
	}

	
	
}
