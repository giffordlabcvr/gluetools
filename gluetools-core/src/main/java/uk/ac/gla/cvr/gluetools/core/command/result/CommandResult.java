package uk.ac.gla.cvr.gluetools.core.command.result;

public abstract class CommandResult {

	public static CommandResult OK = new OkResult();
	
	public abstract void renderToConsole(CommandResultRenderingContext renderCtx);
	
}
