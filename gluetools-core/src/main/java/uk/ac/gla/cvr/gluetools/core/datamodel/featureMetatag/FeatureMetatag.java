package uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatagException.Code;

@GlueDataClass(defaultListedProperties = {FeatureMetatag.NAME_PROPERTY, FeatureMetatag.VALUE_PROPERTY})
public class FeatureMetatag extends _FeatureMetatag {

	public enum Type {
		/** 
		 * boolean
		 * informational features are in place just to group other features
		 * */
		INFORMATIONAL, 
		/** 
		 * boolean
		 * this feature uses its own codon numbering coordinates, rather than inheriting them from an ancestor.
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
		 * true this feature codes for amino acids
		 */
		CODES_AMINO_ACIDS,
		/**
		 * string 
		 * name of a module which labels codons within the feature to a preferred labeling scheme
		 */
		CODON_LABELER_MODULE,
	}

	private Type type = null;
	
	public static final String FEATURE_NAME_PATH = 
			_FeatureMetatag.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String featureName, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	public Type getType() {
		if(type == null) {
			type = buildType();
		}
		return type;
	}
	
	private Type buildType() {
		String name = getName();
		try {
			return Type.valueOf(name);
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
