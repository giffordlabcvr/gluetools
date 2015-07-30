package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class OkResult extends CommandResult {

	private static final String OK_RESULT = "okResult";

	public OkResult() {
		super(GlueXmlUtils.documentWithElement(OK_RESULT).getOwnerDocument());
		JsonUtils.setJsonType(getDocument().getDocumentElement(), JsonType.Object, false);
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderCtx.output("OK");
	}

}
