package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;

@GlueDataClass(defaultListColumns = {
		_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY, 
		_Sequence.SEQUENCE_ID_PROPERTY})
public class Sequence extends _Sequence {

	private SequenceFormat sequenceFormat;
	
	public static Map<String, String> pkMap(String sourceName, String sequenceID) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY, sourceName);
		idMap.put(SEQUENCE_ID_PROPERTY, sequenceID);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setSequenceID(pkMap.get(SEQUENCE_ID_PROPERTY));
	}
	
	public SequenceFormat getSequenceFormat() {
		if(sequenceFormat == null) {
			sequenceFormat = buildSequenceFormat();
		}
		return sequenceFormat;
	}
	
	private SequenceFormat buildSequenceFormat() {
		String format = getFormat();
		try {
			return SequenceFormat.valueOf(format);
		} catch(IllegalArgumentException iae) {
			throw new SequenceException(Code.UNKNOWN_SEQUENCE_FORMAT, format);
		}
	}	
	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getSource().getName(), getSequenceID());
	}

}