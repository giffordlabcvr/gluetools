package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class ShowCreationTimeResult extends MapResult {

	public ShowCreationTimeResult(String creationTime) {
		super("showSequenceResult", mapBuilder()
			.put("creationTime", creationTime));
	}

	public long getCreationTime() {
		return Long.parseLong(getDocumentReader().stringValue("creationTime"));
	}

	
}
