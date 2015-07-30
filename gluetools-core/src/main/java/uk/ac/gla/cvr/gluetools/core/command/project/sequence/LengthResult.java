package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class LengthResult extends CommandResult {

	public LengthResult(int length) {
		super(lengthResultDocument(length));
	}

	private static Document lengthResultDocument(int length) {
		Element rootElem = GlueXmlUtils.documentWithElement("lengthResult");
		JsonUtils.setJsonType(rootElem, JsonType.Object, false);
		GlueXmlUtils.appendElementWithText(rootElem, "length", Integer.toString(length), JsonType.Integer);
		return rootElem.getOwnerDocument();
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		buf.append("Length: ");
		buf.append(GlueXmlUtils.getXPathString(getDocument(), "/lengthResult/length/text()"));
		renderCtx.output(buf.toString());
	}
	
	
	
}
