package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

public class ShowSequenceResult extends MapResult {

	public ShowSequenceResult(String sourceName, String sequenceID) {
		super("showSequenceResult", mapBuilder()
			.put(ReferenceSequence.SEQ_SOURCE_NAME_PATH, sourceName)
			.put(ReferenceSequence.SEQ_ID_PATH, sequenceID));
	}

	public String getSourceName() {
		return getDocumentReader().stringValue(ReferenceSequence.SEQ_SOURCE_NAME_PATH);
	}

	public String getSequenceID() {
		return getDocumentReader().stringValue(ReferenceSequence.SEQ_ID_PATH);
	}

	
}
