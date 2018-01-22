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
