package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class ShowReferenceResult extends CommandResult {

	public ShowReferenceResult(String referenceName) {
		super(createResultDoc(referenceName));
	}

	private static Document createResultDoc(String referenceName) {
		Element rootElem = GlueXmlUtils.documentWithElement("showReferenceResult");
		JsonUtils.setJsonType(rootElem, JsonType.Object, false);
		GlueXmlUtils.appendElementWithText(rootElem, "referenceName", referenceName, JsonType.String);
		return rootElem.getOwnerDocument();
	}

	public String getReferenceName() {
		return GlueXmlUtils.getXPathString(getDocument(), "/showReferenceResult/referenceName/text()");
	}

	
	
}
