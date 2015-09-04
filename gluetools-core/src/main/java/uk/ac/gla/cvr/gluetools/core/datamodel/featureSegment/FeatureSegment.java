package uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;

@GlueDataClass(defaultListColumns = {_FeatureSegment.REF_START_PROPERTY, _FeatureSegment.REF_END_PROPERTY})
public class FeatureSegment extends _FeatureSegment implements IReferenceSegment {
	
	public static final String REF_SEQ_NAME_PATH = 
			_FeatureSegment.FEATURE_LOCATION_PROPERTY+"."+_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+
					_ReferenceSequence.NAME_PROPERTY;

	public static final String FEATURE_NAME_PATH = 
			_FeatureSegment.FEATURE_LOCATION_PROPERTY+"."+FeatureLocation.FEATURE_NAME_PATH;


	
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
				getFeatureLocation().getReferenceSequence().getName(), 
				getFeatureLocation().getFeature().getName(),
				getRefStart(), 
				getRefEnd());
	}

	public FeatureSegment clone() {
		FeatureSegment copy = new FeatureSegment();
		copy.setRefStart(getRefStart());
		copy.setRefEnd(getRefEnd());
		return copy;
	}
}
