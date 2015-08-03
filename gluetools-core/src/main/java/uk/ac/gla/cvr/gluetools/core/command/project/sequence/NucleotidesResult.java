package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class NucleotidesResult extends CommandResult {

	public NucleotidesResult(int beginIndex, int endIndex, String nucleotides) {
		super(nucleotidesResultDocument(beginIndex, endIndex, nucleotides));
	}

	private static Document nucleotidesResultDocument(int beginIndex, int endIndex, String nucleotides) {
		Element rootElem = GlueXmlUtils.documentWithElement("nucleotidesResult");
		JsonUtils.setJsonType(rootElem, JsonType.Object, false);	
		GlueXmlUtils.appendElementWithText(rootElem, "beginIndex", Integer.toString(beginIndex), JsonType.Integer);
		GlueXmlUtils.appendElementWithText(rootElem, "endIndex", Integer.toString(endIndex), JsonType.Integer);
		GlueXmlUtils.appendElementWithText(rootElem, "nucleotides", nucleotides, JsonType.String);
		return rootElem.getOwnerDocument();
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		buf.append("beginIndex: ").append(GlueXmlUtils.getXPathString(getDocument(), "/nucleotidesResult/beginIndex/text()"));
		buf.append(", endIndex: ").append(GlueXmlUtils.getXPathString(getDocument(), "/nucleotidesResult/endIndex/text()"));;
		buf.append("\n").append(GlueXmlUtils.getXPathString(getDocument(), "/nucleotidesResult/nucleotides/text()"));;
		renderCtx.output(buf.toString());
	}

	public String getNucleotides() {
		return GlueXmlUtils.getXPathString(getDocument(), "/nucleotidesResult/nucleotides/text()");
	}
	
}
