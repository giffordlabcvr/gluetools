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
