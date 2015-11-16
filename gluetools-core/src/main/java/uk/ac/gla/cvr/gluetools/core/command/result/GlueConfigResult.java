package uk.ac.gla.cvr.gluetools.core.command.result;

public class GlueConfigResult extends OkResult {

	private boolean outputToConsole;
	
	private String glueConfig;
	
	public GlueConfigResult(boolean outputToConsole, String glueConfig) {
		super();
		this.outputToConsole = outputToConsole;
		this.glueConfig = glueConfig;
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		if(outputToConsole) {
			renderCtx.output(glueConfig);
		} else {
			super.renderToConsoleAsText(renderCtx);
		}
	}

}
