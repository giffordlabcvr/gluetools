package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;

public class GlueConfigResult extends OkResult {

	private boolean outputToConsole;
	
	private String glueConfig;
	
	private GlueConfigResult(boolean outputToConsole, String glueConfig) {
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
	
	public static GlueConfigResult generateGlueConfigResult(CommandContext cmdContext, String fileName, String glueConfig) {
		boolean outputToConsole = false;
		if(fileName == null) {
			outputToConsole = true;
		} else {
			ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
			consoleCmdContext.saveBytes(fileName, glueConfig.getBytes());
		}
		GlueConfigResult glueConfigResult = new GlueConfigResult(outputToConsole, glueConfig);
		return glueConfigResult;
	}

	
}