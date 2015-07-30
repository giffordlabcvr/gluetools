package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class OkResult extends CommandResult {

	private static final String OK_RESULT = "okResult";

	public OkResult() {
		super(XmlUtils.documentWithElement(OK_RESULT).getOwnerDocument());
		JsonUtils.setJsonType(getDocument().getDocumentElement(), JsonType.Object, false);
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderCtx.output("OK");
	}

}
