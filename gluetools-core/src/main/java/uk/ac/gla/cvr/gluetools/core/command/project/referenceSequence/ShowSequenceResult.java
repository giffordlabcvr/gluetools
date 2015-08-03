package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class ShowSequenceResult extends CommandResult {

	public ShowSequenceResult(String sourceName, String sequenceID) {
		super(createResultDoc(sourceName, sequenceID));
	}

	private static Document createResultDoc(String sourceName, String sequenceID) {
		Element rootElem = GlueXmlUtils.documentWithElement("showSequenceResult");
		JsonUtils.setJsonType(rootElem, JsonType.Object, false);
		GlueXmlUtils.appendElementWithText(rootElem, "sourceName", sourceName, JsonType.String);
		GlueXmlUtils.appendElementWithText(rootElem, "sequenceID", sequenceID, JsonType.String);
		return rootElem.getOwnerDocument();
	}

	public String getSourceName() {
		return GlueXmlUtils.getXPathString(getDocument(), "/showSequenceResult/sourceName/text()");
	}

	public String getSequenceID() {
		return GlueXmlUtils.getXPathString(getDocument(), "/showSequenceResult/sequenceID/text()");
	}

	
	
}
