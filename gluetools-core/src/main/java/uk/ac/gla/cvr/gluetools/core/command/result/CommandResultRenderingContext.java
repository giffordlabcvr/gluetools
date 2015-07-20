package uk.ac.gla.cvr.gluetools.core.command.result;

public interface CommandResultRenderingContext {

	public void output(String outputLines);
	
	public boolean showCmdXml();
}
