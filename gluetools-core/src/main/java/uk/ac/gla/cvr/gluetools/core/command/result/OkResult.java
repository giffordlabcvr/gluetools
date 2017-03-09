package uk.ac.gla.cvr.gluetools.core.command.result;


public class OkResult extends MapResult {

	private static final String OK_RESULT = "okResult";

	public OkResult() {
		this(mapBuilder());
	}

	protected OkResult(MapBuilder mapBuilder) {
		super(OK_RESULT, mapBuilder);
	}

	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		renderCtx.output("OK");
	}

}
