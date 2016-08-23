package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;

public class ShowMappingExtractedResult extends ListResult {

	private static final String GLUE_FIELD_REQUIREMENT = "fieldRequirement";
	private static final String GLUE_SEQUENCE_FIELD = "glueSequenceField";
	private static final String DIGS_EXTRACTED_FIELD = "digsExtractedField";

	public ShowMappingExtractedResult(CommandContext cmdContext, List<ImportExtractedFieldRule> rules) {
		super(cmdContext, ImportExtractedFieldRule.class, rules, Arrays.asList(DIGS_EXTRACTED_FIELD, GLUE_SEQUENCE_FIELD, GLUE_FIELD_REQUIREMENT), 
				new BiFunction<ImportExtractedFieldRule, String, Object>() {
			@Override
			public Object apply(ImportExtractedFieldRule rule, String column) {
				if(column.equals(DIGS_EXTRACTED_FIELD)) {
					return rule.getExtractedField();
				} else if(column.equals(GLUE_SEQUENCE_FIELD)) {
					return rule.getSequenceFieldToUse();
				} else if(column.equals(GLUE_FIELD_REQUIREMENT)) {
					return rule.getGlueFieldRequirement().toString();
				}
				return null;
			}
		});
	}

}
