package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;

@GlueDataClass(
		defaultListedProperties = {Sequence.SOURCE_NAME_PATH, _Sequence.SEQUENCE_ID_PROPERTY },
		listableBuiltInProperties = {Sequence.SOURCE_NAME_PATH, _Sequence.SEQUENCE_ID_PROPERTY, _Sequence.FORMAT_PROPERTY } )
public class Sequence extends _Sequence {

	public static final String SOURCE_NAME_PATH = _Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	private SequenceFormat sequenceFormat;
	private AbstractSequenceObject sequenceObject;
	
	public static Map<String, String> pkMap(String sourceName, String sequenceID) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(SOURCE_NAME_PATH, sourceName);
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
	public Map<String, String> pkMap() {
		return pkMap(getSource().getName(), getSequenceID());
	}

	public AbstractSequenceObject getSequenceObject() {
		if(sequenceObject == null) {
			sequenceObject = buildSequenceObject();
		}
		return sequenceObject;
	}

	private AbstractSequenceObject buildSequenceObject() {
		AbstractSequenceObject sequenceObject = getSequenceFormat().sequenceObject();
		sequenceObject.fromPackedData(getSeqOrigData().getPackedData());
		return sequenceObject;
	}
	
	public byte[] getOriginalData() {
		return getSequenceObject().toOriginalData();
	}
	
	public void setOriginalData(byte[] originalData) {
		AbstractSequenceObject sequenceObject = getSequenceFormat().sequenceObject();
		sequenceObject.fromOriginalData(originalData);
		getSeqOrigData().setPackedData(sequenceObject.toPackedData());
		this.sequenceObject = sequenceObject;
	}
	
}