package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class NucleotidesResult extends CommandResult {

	public NucleotidesResult(int beginIndex, int endIndex, String nucleotides) {
		super("nucleotidesResult");
		DocumentBuilder documentBuilder = getDocumentBuilder();
		documentBuilder.setInt("beginIndex", beginIndex);
		documentBuilder.setInt("endIndex", endIndex);
		documentBuilder.setString("nucleotides", nucleotides);
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
