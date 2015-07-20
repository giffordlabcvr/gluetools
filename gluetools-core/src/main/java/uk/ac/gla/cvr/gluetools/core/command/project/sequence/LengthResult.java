package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class LengthResult extends DocumentResult {

	public LengthResult(int length) {
		super(lengthResultDocument(length));
	}

	private static Document lengthResultDocument(int length) {
		Element rootElem = XmlUtils.documentWithElement("lengthResult");
		XmlUtils.appendElementWithText(rootElem, "length", Integer.toString(length));
		return rootElem.getOwnerDocument();
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		buf.append("Length: ");
		buf.append(XmlUtils.getXPathString(getDocument(), "/lengthResult/length/text()"));
		renderCtx.output(buf.toString());
	}
	
	
	
}
