package uk.ac.gla.cvr.gluetools.core.datamodel.alignment;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;

@GlueDataClass(defaultListColumns = {_Alignment.NAME_PROPERTY, Alignment.REF_SEQ_SOURCE_NAME_PATH, Alignment.REF_SEQ_ID_PATH})
public class Alignment extends _Alignment {
	
	public static final String REF_SEQ_SOURCE_NAME_PATH = 
			_Alignment.REF_SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String REF_SEQ_ID_PATH = 
			_Alignment.REF_SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;

	
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
