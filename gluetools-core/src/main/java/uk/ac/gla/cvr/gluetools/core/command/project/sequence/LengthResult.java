package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class LengthResult extends CommandResult {

	public LengthResult(int length) {
		super("lengthResult");
		getDocumentBuilder().setInt("length", length);
	}


	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringBuffer buf = new StringBuffer();
		buf.append("Length: ");
		buf.append(GlueXmlUtils.getXPathString(getDocument(), "/lengthResult/length/text()"));
		renderCtx.output(buf.toString());
	}
	
	
	
}
