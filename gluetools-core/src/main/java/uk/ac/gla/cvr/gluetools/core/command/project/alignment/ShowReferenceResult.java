package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class ShowReferenceResult extends CommandResult {

	public ShowReferenceResult(String referenceName) {
		super("showReferenceResult");
		getDocumentBuilder().setString("referenceName", referenceName);
	}

	public String getReferenceName() {
		return GlueXmlUtils.getXPathString(getDocument(), "/showReferenceResult/referenceName/text()");
	}
	
}
