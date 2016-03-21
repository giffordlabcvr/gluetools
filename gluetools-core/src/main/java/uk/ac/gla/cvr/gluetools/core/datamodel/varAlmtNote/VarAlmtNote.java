package uk.ac.gla.cvr.gluetools.core.datamodel.varAlmtNote;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._VarAlmtNote;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@GlueDataClass(
		defaultListedProperties = { VarAlmtNote.REF_SEQ_NAME_PATH, VarAlmtNote.FEATURE_NAME_PATH, VarAlmtNote.VARIATION_NAME_PATH, VarAlmtNote.ALIGNMENT_NAME_PATH },
		listableBuiltInProperties = { VarAlmtNote.REF_SEQ_NAME_PATH, VarAlmtNote.FEATURE_NAME_PATH, VarAlmtNote.VARIATION_NAME_PATH, VarAlmtNote.ALIGNMENT_NAME_PATH },
		modifiableBuiltInProperties = { })		
public class VarAlmtNote extends _VarAlmtNote {

	public static final String ALIGNMENT_NAME_PATH = _VarAlmtNote.ALIGNMENT_PROPERTY+"."+_Alignment.NAME_PROPERTY;
	public static final String REF_SEQ_NAME_PATH = _VarAlmtNote.VARIATION_PROPERTY+"."+Variation.REF_SEQ_NAME_PATH;
	public static final String FEATURE_NAME_PATH = _VarAlmtNote.VARIATION_PROPERTY+"."+Variation.FEATURE_NAME_PATH;
	public static final String VARIATION_NAME_PATH = _VarAlmtNote.VARIATION_PROPERTY+"."+_Variation.NAME_PROPERTY;

	public static Map<String, String> pkMap(String alignmentName, String referenceName, String featureName, String variationName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(REF_SEQ_NAME_PATH, referenceName);
		idMap.put(FEATURE_NAME_PATH, featureName);
		idMap.put(VARIATION_NAME_PATH, variationName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getAlignment().getName(), 
				getVariation().getFeatureLoc().getReferenceSequence().getName(), 
				getVariation().getFeatureLoc().getFeature().getName(), 
				getVariation().getName());
	}

}
