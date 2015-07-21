package uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureSegment;

@GlueDataClass(defaultListColumns = {_FeatureSegment.REF_START_PROPERTY, _FeatureSegment.REF_END_PROPERTY})
public class FeatureSegment extends _FeatureSegment {
	
	public static final String ALIGNMENT_NAME_PATH = 
			_FeatureSegment.FEATURE_PROPERTY+"."+_Feature.ALIGNMENT_PROPERTY+"."+
					_Alignment.NAME_PROPERTY;

	public static final String FEATURE_NAME_PATH = 
			_FeatureSegment.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;


	
	public static Map<String, String> pkMap(String alignmentName, String featureName, 
			int refStart, int refEnd) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(REF_START_PROPERTY, Integer.toString(refStart));
		idMap.put(REF_END_PROPERTY, Integer.toString(refEnd));
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setRefStart(Integer.parseInt(pkMap.get(REF_START_PROPERTY)));
		setRefEnd(Integer.parseInt(pkMap.get(REF_END_PROPERTY)));
	}
	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(
				getFeature().getAlignment().getName(), 
				getFeature().getName(),
				getRefStart(), 
				getRefEnd());
	}

}
