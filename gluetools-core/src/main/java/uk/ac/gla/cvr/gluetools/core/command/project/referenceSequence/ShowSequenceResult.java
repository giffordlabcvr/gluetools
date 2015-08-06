package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class ShowSequenceResult extends CommandResult {

	public ShowSequenceResult(String sourceName, String sequenceID) {
		super("showSequenceResult");
		getDocumentBuilder()
			.setString("sourceName", sourceName)
			.setString("sequenceID", sequenceID);
	}

	public String getSourceName() {
		return GlueXmlUtils.getXPathString(getDocument(), "/showSequenceResult/sourceName/text()");
	}

	public String getSequenceID() {
		return GlueXmlUtils.getXPathString(getDocument(), "/showSequenceResult/sequenceID/text()");
	}

	
	
}
