package uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureMetatag;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatagException.Code;

@GlueDataClass(defaultListColumns = {FeatureMetatag.NAME_PROPERTY})
public class FeatureMetatag extends _FeatureMetatag {

	public enum Type {
		OPEN_READING_FRAME, // this feature (and any descendent features) can be translated to amino acids
		INFORMATIONAL, // informational features are in place just to group other features
		HIDDEN, // hidden features should not be displayed in a UI
		OWN_CODON_NUMBERING, // this feature uses its own codon numbering coordinates, rather than inheriting them from an ancestor.		
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
	protected Map<String, String> pkMap() {
		return pkMap(getFeature().getName(), getName());
	}

	
}
