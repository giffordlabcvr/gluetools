package uk.ac.gla.cvr.gluetools.core.datamodel.positionVariation;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._PositionVariation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;

public class PositionVariation extends _PositionVariation {

	
	public static final String CACHE_GROUP = "PositionVariation";
	
	public static final String FEATURE_NAME_PATH = _PositionVariation.FEATURE_LOCATION_PROPERTY+"."+_FeatureLocation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;
	public static final String REF_SEQ_NAME_PATH = _PositionVariation.FEATURE_LOCATION_PROPERTY+"."+_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;
	public static final String VARIATION_NAME_PATH = _PositionVariation.VARIATION_PROPERTY+"."+_Variation.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String referenceName, String featureName, String variationName, Integer position, TranslationFormat translationFormat) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(REF_SEQ_NAME_PATH, referenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(VARIATION_NAME_PATH, variationName);
		idMap.put(POSITION_PROPERTY, Integer.toString(position));
		idMap.put(TRANSLATION_TYPE_PROPERTY, translationFormat.name());
		return idMap;
	}
	

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setPosition(Integer.parseInt(pkMap.get(POSITION_PROPERTY)));
		setTranslationType(pkMap.get(TRANSLATION_TYPE_PROPERTY));
	}

	public TranslationFormat getTranslationFormat() {
		return TranslationFormat.valueOf(getTranslationType());
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getFeatureLocation().getReferenceSequence().getName(), 
				getFeatureLocation().getFeature().getName(), 
				getVariation().getName(), 
				getPosition(), 
				getTranslationFormat());
	}


}
