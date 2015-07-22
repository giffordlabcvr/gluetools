package uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;

@GlueDataClass(defaultListColumns = {_FeatureSegment.REF_START_PROPERTY, _FeatureSegment.REF_END_PROPERTY})
public class FeatureSegment extends _FeatureSegment {
	
	public static final String REF_SEQ_NAME_PATH = 
			_FeatureSegment.FEATURE_PROPERTY+"."+_Feature.REFERENCE_SEQUENCE_PROPERTY+"."+
					_ReferenceSequence.NAME_PROPERTY;

	public static final String FEATURE_NAME_PATH = 
			_FeatureSegment.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;


	
	public static Map<String, String> pkMap(String refSeqName, String featureName, 
			int refStart, int refEnd) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(REF_SEQ_NAME_PATH, refSeqName);
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
				getFeature().getReferenceSequence().getName(), 
				getFeature().getName(),
				getRefStart(), 
				getRefEnd());
	}

}
