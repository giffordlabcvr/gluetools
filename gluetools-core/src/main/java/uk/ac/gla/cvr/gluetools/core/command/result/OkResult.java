package uk.ac.gla.cvr.gluetools.core.command.result;


public class OkResult extends CommandResult {

	private static final String OK_RESULT = "okResult";

	public OkResult() {
		super(OK_RESULT);
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderCtx.output("OK");
	}

}
