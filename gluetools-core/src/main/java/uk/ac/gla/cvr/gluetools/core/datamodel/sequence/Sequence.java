package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;

@GlueDataClass(listColumnHeaders = {_Sequence.SOURCE_PROPERTY, _Sequence.SEQUENCE_ID_PROPERTY, _Sequence.FORMAT_PROPERTY})
public class Sequence extends _Sequence {

	private SequenceFormat sequenceFormat;
	private Document sequenceDoc;
	
	@Override
	public String[] populateListRow() {
		return new String[]{getSource().getName(), getSequenceID(), getFormat()};
	}

	public static Map<String, String> pkMap(String sourceName, String sourceId) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(SOURCE_PK_COLUMN, sourceName);
		idMap.put(SEQUENCE_ID_PK_COLUMN, sourceId);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setSequenceID(pkMap.get(SEQUENCE_ID_PK_COLUMN));
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

	public Document getSequenceDoc() {
		if(sequenceDoc == null) {
			sequenceDoc = buildSequenceDoc();
		}
		return sequenceDoc;
	}
	
	private Document buildSequenceDoc() {
		return getSequenceFormat().asXml(getData());
	}
}