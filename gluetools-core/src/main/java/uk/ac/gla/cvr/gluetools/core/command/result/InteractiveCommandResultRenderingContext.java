package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.InputStream;

public interface InteractiveCommandResultRenderingContext extends CommandResultRenderingContext {

	public int getTerminalWidth();
	
	public int getTerminalHeight();

	public InputStream getInputStream();
	
	public boolean interactiveTables();
	
	public int getTableTruncationLimit();
	
}
