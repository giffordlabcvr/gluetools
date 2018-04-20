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
package uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._VariationMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatagException.Code;

@GlueDataClass(defaultListedProperties = {VariationMetatag.NAME_PROPERTY, VariationMetatag.VALUE_PROPERTY})
public class VariationMetatag extends _VariationMetatag {
	
	public static final String VARIATION_NAME_PATH = _VariationMetatag.VARIATION_PROPERTY+"."+_Variation.NAME_PROPERTY;
	public static final String FEATURE_NAME_PATH = _VariationMetatag.VARIATION_PROPERTY+"."+_Variation.FEATURE_LOC_PROPERTY+"."+_FeatureLocation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;
	public static final String REF_SEQ_NAME_PATH = _VariationMetatag.VARIATION_PROPERTY+"."+_Variation.FEATURE_LOC_PROPERTY+"."+_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getVariation().getFeatureLoc().getReferenceSequence().getName(), 
				getVariation().getFeatureLoc().getFeature().getName(), 
				getVariation().getName(), getName());
	}

	public static Map<String, String> pkMap(String refSeqName, String featureName, String variationName, String name) {
		Map<String, String> pkMap = new LinkedHashMap<String, String>();
		pkMap.put(REF_SEQ_NAME_PATH, refSeqName);
		pkMap.put(FEATURE_NAME_PATH, featureName);
		pkMap.put(VARIATION_NAME_PATH, variationName);
		pkMap.put(NAME_PROPERTY, name);
		return pkMap;
	}
	

	public enum VariationMetatagType {
		SIMPLE_NT_PATTERN,
		REGEX_NT_PATTERN,
		SIMPLE_AA_PATTERN,
		REGEX_AA_PATTERN,
		FLANKING_AAS,
		FLANKING_NTS,
		MIN_COMBINED_TRIPLET_FRACTION,
		MIN_COMBINED_NT_FRACTION,
		MIN_DELETION_LENGTH_AAS,
		MIN_DELETION_LENGTH_NTS,
		MAX_DELETION_LENGTH_AAS,
		MAX_DELETION_LENGTH_NTS,
		MIN_INSERTION_LENGTH_AAS,
		MIN_INSERTION_LENGTH_NTS,
		MAX_INSERTION_LENGTH_AAS,
		MAX_INSERTION_LENGTH_NTS,
		CONJUNCT_NAME_1,
		CONJUNCT_NAME_2,
		CONJUNCT_NAME_3,
		CONJUNCT_NAME_4,
		CONJUNCT_NAME_5,
		
	}

	private VariationMetatagType type = null;
	

	public VariationMetatagType getType() {
		if(type == null) {
			type = buildType();
		}
		return type;
	}
	
	private VariationMetatagType buildType() {
		String name = getName();
		try {
			return VariationMetatagType.valueOf(name);
		} catch(IllegalArgumentException iae) {
			throw new VariationMetatagException(Code.UNKNOWN_VARIATION_METATAG, name);
		}
	}
	
	@Override
	public void setName(String name) {
		super.setName(name);
		this.type = null;
	}

	
	public VariationMetatag clone() {
		throw new RuntimeException("VariationMetatag.clone() not supported");
	}

	
}
