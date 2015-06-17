package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

public class ConsoleCommandContext extends CommandContext {

	public ConsoleCommandContext(GluetoolsEngine gluetoolsEngine) {
		super(gluetoolsEngine);
	}

	private boolean finished = false;

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	

	
}
