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
	public static final String GLUE_FIELD_REQUIREMENT = "glueFieldRequirement";

	
	public enum GlueFieldRequirement {
		WARN, 
		IGNORE, 
		REQUIRE
	}

	private String extractedField;
	private String sequenceField;
	private GlueFieldRequirement glueFieldRequirement = GlueFieldRequirement.WARN;

	
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
		glueFieldRequirement = Optional.of(PluginUtils
				.configureEnumProperty(GlueFieldRequirement.class, configElem, GLUE_FIELD_REQUIREMENT, false)).orElse(GlueFieldRequirement.WARN);
		
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
		sequenceField.append("digs_");
		if(extractedField.contains("_")) {
			sequenceField.append(extractedField.toLowerCase());
		} else {
			for(int i = 0; i < extractedField.length(); i++) {
				char c = extractedField.charAt(i);
				if(i > 0 && Character.isUpperCase(c) 
						&& Character.isLowerCase(extractedField.charAt(i-1))) {
					sequenceField.append("_");
				}
				sequenceField.append(Character.toLowerCase(c));
			}
		}
		return sequenceField.toString();
	}

	public GlueFieldRequirement getGlueFieldRequirement() {
		return glueFieldRequirement;
	}

	
}
