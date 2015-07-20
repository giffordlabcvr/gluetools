package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class OkResult extends DocumentResult {

	private static final String OK_RESULT = "okResult";

	public OkResult() {
		super(XmlUtils.documentWithElement(OK_RESULT).getOwnerDocument());
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderCtx.output("OK");
	}

}
