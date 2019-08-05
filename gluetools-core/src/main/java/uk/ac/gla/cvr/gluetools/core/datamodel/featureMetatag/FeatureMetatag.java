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
package uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatagException.Code;

@GlueDataClass(defaultListedProperties = {FeatureMetatag.NAME_PROPERTY, FeatureMetatag.VALUE_PROPERTY})
public class FeatureMetatag extends _FeatureMetatag {

	public enum FeatureMetatagType {
		/** 
		 * boolean
		 * informational features are in place just to group other features
		 * */
		INFORMATIONAL, 
		/** 
		 * boolean
		 * this feature uses its own codon numbering coordinates, rather than inheriting them from an ancestor.
		 * In fact all codon definitions are defined by the ancestor.
		 */
		OWN_CODON_NUMBERING, 
		/** 
		 * boolean
		 * include this feature in the summary view
		 */
		INCLUDE_IN_SUMMARY, 
		/** 
		 * integer
		 * what order should this feature be displayed in, relative to other features with the same parent
		 */
		DISPLAY_ORDER, 
		/**
		 * boolean 
		 * true iff this feature codes for amino acids
		 */
		CODES_AMINO_ACIDS,
		/**
		 * boolean 
		 * true iff the protein translation results from the reverse complement of the feature location
		 * Useful for ambisense genomes
		 */
		REVERSE_COMPLEMENT_TRANSLATION,
		/**
		 * string 
		 * name of a module which labels codons within the feature to a preferred labeling scheme
		 */
		CODON_LABELER_MODULE,
		/**
		 * boolean
		 * if true, this feature bridges the cut-off point in a circular genome.
		 */
		CIRCULAR_BRIDGING,
		
	}

	private FeatureMetatagType type = null;
	
	public static final String FEATURE_NAME_PATH = 
			_FeatureMetatag.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String featureName, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	public FeatureMetatagType getType() {
		if(type == null) {
			type = buildType();
		}
		return type;
	}
	
	private FeatureMetatagType buildType() {
		String name = getName();
		try {
			return FeatureMetatagType.valueOf(name);
		} catch(IllegalArgumentException iae) {
			throw new FeatureMetatagException(Code.UNKNOWN_FEATURE_METATAG, name);
		}
	}
	
	@Override
	public void setName(String name) {
		super.setName(name);
		this.type = null;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getFeature().getName(), getName());
	}

	
}
