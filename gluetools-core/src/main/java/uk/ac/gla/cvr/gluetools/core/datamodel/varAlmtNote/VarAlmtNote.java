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
package uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@GlueDataClass(
		defaultListedProperties = { VarAlmtNote.REF_SEQ_NAME_PATH, VarAlmtNote.FEATURE_NAME_PATH, VarAlmtNote.VARIATION_NAME_PATH, VarAlmtNote.ALIGNMENT_NAME_PATH },
		listableBuiltInProperties = { VarAlmtNote.REF_SEQ_NAME_PATH, VarAlmtNote.FEATURE_NAME_PATH, VarAlmtNote.VARIATION_NAME_PATH, VarAlmtNote.ALIGNMENT_NAME_PATH },
		modifiableBuiltInProperties = { })		
public class VarAlmtNote extends _VarAlmtNote {

	public static final String ALIGNMENT_NAME_PATH = _VarAlmtNote.ALIGNMENT_PROPERTY+"."+_Alignment.NAME_PROPERTY;
	public static final String REF_SEQ_NAME_PATH = _VarAlmtNote.VARIATION_PROPERTY+"."+Variation.REF_SEQ_NAME_PATH;
	public static final String FEATURE_NAME_PATH = _VarAlmtNote.VARIATION_PROPERTY+"."+Variation.FEATURE_NAME_PATH;
	public static final String VARIATION_NAME_PATH = _VarAlmtNote.VARIATION_PROPERTY+"."+_Variation.NAME_PROPERTY;

	public static Map<String, String> pkMap(String alignmentName, String referenceName, String featureName, String variationName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(REF_SEQ_NAME_PATH, referenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(VARIATION_NAME_PATH, variationName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getAlignment().getName(), 
				getVariation().getFeatureLoc().getReferenceSequence().getName(), 
				getVariation().getFeatureLoc().getFeature().getName(), 
				getVariation().getName());
	}

}
