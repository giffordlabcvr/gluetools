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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		metaTags = {  }, 
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
