package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DocumentResult;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class NucleotidesResult extends DocumentResult {

	public NucleotidesResult(int beginIndex, int endIndex, String nucleotides) {
		super(nucleotidesResultDocument(beginIndex, endIndex, nucleotides));
	}

	private static Document nucleotidesResultDocument(int beginIndex, int endIndex, String nucleotides) {
		Element rootElem = XmlUtils.documentWithElement("nucleotidesResult");
		XmlUtils.appendElementWithText(rootElem, "beginIndex", Integer.toString(beginIndex));
		XmlUtils.appendElementWithText(rootElem, "endIndex", Integer.toString(endIndex));
		XmlUtils.appendElementWithText(rootElem, "nucleotides", nucleotides);
		return rootElem.getOwnerDocument();
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		buf.append("beginIndex: ").append(XmlUtils.getXPathString(getDocument(), "/nucleotidesResult/beginIndex/text()"));
		buf.append(", endIndex: ").append(XmlUtils.getXPathString(getDocument(), "/nucleotidesResult/endIndex/text()"));;
		buf.append("\n").append(XmlUtils.getXPathString(getDocument(), "/nucleotidesResult/nucleotides/text()"));;
		renderCtx.output(buf.toString());
	}
	
	
	
}
