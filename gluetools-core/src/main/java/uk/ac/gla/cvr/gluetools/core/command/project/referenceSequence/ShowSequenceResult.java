package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class ShowSequenceResult extends MapResult {

	public ShowSequenceResult(String sourceName, String sequenceID) {
		super("showSequenceResult", mapBuilder()
			.put("sourceName", sourceName)
			.put("sequenceID", sequenceID));
	}

	public String getSourceName() {
		return getDocumentReader().stringValue("sourceName");
	}

	public String getSequenceID() {
		return getDocumentReader().stringValue("sequenceID");
	}

	
}
