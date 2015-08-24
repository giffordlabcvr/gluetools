package uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;

@GlueDataClass(defaultListColumns = {FeatureLocation.FEATURE_NAME_PATH})
public class FeatureLocation extends _FeatureLocation {
	
	public static final String REF_SEQ_NAME_PATH = 
			_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;
	public static final String FEATURE_NAME_PATH = 
			_FeatureLocation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String referenceSequenceName, String featureName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(REF_SEQ_NAME_PATH, referenceSequenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}

	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getReferenceSequence().getName(), getFeature().getName());
	}

}

