package uk.ac.gla.cvr.gluetools.core.datamodel.feature;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Feature;

@GlueDataClass(defaultListColumns = {_Feature.NAME_PROPERTY, _Feature.DESCRIPTION_PROPERTY})
public class Feature extends _Feature {
	
	public static final String ALIGNMENT_NAME_PATH = 
			_Feature.ALIGNMENT_PROPERTY+"."+_Alignment.NAME_PROPERTY;

	
	public static Map<String, String> pkMap(String alignmentName, String featureName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(NAME_PROPERTY, featureName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getAlignment().getName(), getName());
	}

}
