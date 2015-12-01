package uk.ac.gla.cvr.gluetools.core.datamodel.vcatMembership;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._VariationCategory;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._VcatMembership;

@GlueDataClass(defaultListColumns = { 
		VcatMembership.VARIATION_REFSEQ_NAME_PATH, VcatMembership.VARIATION_FEATURE_NAME_PATH,
		VcatMembership.VARIATION_NAME_PATH, VcatMembership.CATEGORY_NAME_PATH})
public class VcatMembership extends _VcatMembership {

	public static final String CACHE_GROUP = "VcatMembership";
	

	public static final String VARIATION_FEATURE_NAME_PATH = 
			_VcatMembership.VARIATION_PROPERTY+"."+_Variation.FEATURE_LOC_PROPERTY+"."+_FeatureLocation.FEATURE_PROPERTY+"."+_Feature.NAME_PROPERTY;

	public static final String VARIATION_REFSEQ_NAME_PATH = 
			_VcatMembership.VARIATION_PROPERTY+"."+_Variation.FEATURE_LOC_PROPERTY+"."+_FeatureLocation.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;

	public static final String VARIATION_NAME_PATH = 
			_VcatMembership.VARIATION_PROPERTY+"."+_Variation.NAME_PROPERTY;

	public static final String CATEGORY_NAME_PATH = 
			_VcatMembership.CATEGORY_PROPERTY+"."+_VariationCategory.NAME_PROPERTY;

	public static final String CATEGORY_DESCRIPTION_PATH = 
			_VcatMembership.CATEGORY_PROPERTY+"."+_VariationCategory.DESCRIPTION_PROPERTY;


	public static Map<String, String> pkMap(String variationRefSeqName, String variationFeatureName, String variationName, String vcatName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(VARIATION_REFSEQ_NAME_PATH, variationRefSeqName);
		idMap.put(VARIATION_FEATURE_NAME_PATH, variationFeatureName);
		idMap.put(VARIATION_NAME_PATH, variationName);
		idMap.put(CATEGORY_NAME_PATH, vcatName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getVariation().getFeatureLoc().getReferenceSequence().getName(), 
				getVariation().getFeatureLoc().getFeature().getName(), 
				getVariation().getName(), getCategory().getName());
	}

}
