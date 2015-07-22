package uk.ac.gla.cvr.gluetools.core.datamodel.feature;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ReferenceSequence;

@GlueDataClass(defaultListColumns = {_Feature.NAME_PROPERTY, _Feature.DESCRIPTION_PROPERTY})
public class Feature extends _Feature {
	
	public static final String REF_SEQ_NAME_PATH = 
			_Feature.REFERENCE_SEQUENCE_PROPERTY+"."+_ReferenceSequence.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String referenceSequenceName, String featureName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(REF_SEQ_NAME_PATH, referenceSequenceName);
		idMap.put(NAME_PROPERTY, featureName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getReferenceSequence().getName(), getName());
	}

}
