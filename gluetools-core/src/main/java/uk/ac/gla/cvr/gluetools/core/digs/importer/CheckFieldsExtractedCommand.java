package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.digs.importer.ImportExtractedFieldRule.GlueFieldRequirement;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

@CommandClass(
		commandWords={"check", "fields", "extracted"}, 
		description = "Check the GLUE sequence table fields are correct for DIGS \"Extracted\" data", 
		docoptUsages = { "" },
		docCategory = "Type-specific module commands",
		docoptOptions = {
		},
		furtherHelp = ""
)
public class CheckFieldsExtractedCommand extends DigsImporterFieldsCommand<OkResult> {

	
	@Override
	protected OkResult execute(CommandContext cmdContext, DigsImporter digsImporter) {
		checkFields(cmdContext, digsImporter);
		return new OkResult();
	}

	public static void checkFields(CommandContext cmdContext, DigsImporter digsImporter) {
		List<ImportExtractedFieldRule> importExtractedFieldRules = digsImporter.getImportExtractedFieldRules();
		try(ModeCloser modeCloser = cmdContext.pushCommandMode("table", "sequence")) {
			Map<String, Map<String, Object>> glueSeqFieldNameToFieldProperties = getExistingFieldProperties(cmdContext);
			for(ImportExtractedFieldRule rule: importExtractedFieldRules) {
				String extractedField = rule.getExtractedField();
				GlueFieldRequirement fieldRequirement = rule.getGlueFieldRequirement();
				if(fieldRequirement == GlueFieldRequirement.IGNORE) {
					continue;
				} else {
					String sequenceFieldToUse = rule.getSequenceFieldToUse();
					GlueFieldInfo requiredGlueField = extractedToGlueFieldInfo.get(extractedField);
					Map<String, Object> actualGlueField = glueSeqFieldNameToFieldProperties.get(sequenceFieldToUse);
					String requiredType = requiredGlueField.glueType;
					Integer requiredLength = requiredGlueField.glueVarcharLength;
					if(actualGlueField == null) {
						if(fieldRequirement == GlueFieldRequirement.WARN) {
							GlueLogger.getGlueLogger().warning("No such sequence field: "+sequenceFieldToUse+" available to map DIGS Extracted field "+extractedField);
						} else {
							throw new DigsImporterException(DigsImporterException.Code.NO_SUCH_SEQUENCE_FIELD,
									extractedField, sequenceFieldToUse);
						}
					} else {
						checkForFieldConflicts(extractedField,
								sequenceFieldToUse, actualGlueField,
								requiredType, requiredLength);
					}
				}
			}
		}
	}


	
}
