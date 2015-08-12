package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class ShowReferenceResult extends MapResult {

	public ShowReferenceResult(String referenceName) {
		super("showReferenceResult", mapBuilder().put("referenceName", referenceName));
	}

	public String getReferenceName() {
		return getDocumentReader().stringValue("referenceName");
	}
	
}
