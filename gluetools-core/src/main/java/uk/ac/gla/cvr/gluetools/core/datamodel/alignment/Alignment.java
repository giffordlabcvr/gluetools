package uk.ac.gla.cvr.gluetools.core.datamodel.alignment;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@GlueDataClass(defaultListColumns = {_Alignment.NAME_PROPERTY, Alignment.REF_SEQ_NAME_PATH})
public class Alignment extends _Alignment {
	
	public static final String REF_SEQ_NAME_PATH = 
			_Alignment.REF_SEQUENCE_PROPERTY+"."+ReferenceSequence.NAME_PROPERTY;
	
	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}
	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getName());
	}

}
