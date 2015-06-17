package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public abstract class CommandMode {
	
	// TODO need to enhance this so that Console commands are only available 
	// for a given mode when we are using the console.
	public static CommandMode ROOT = new CommandMode("GLUE", PluginFactory.get(RootCommandFactory.creator)){};
	
	private String modeId;
	
	private CommandFactory commandFactory;

	protected CommandMode(String modeId, CommandFactory commandFactory) {
		super();
		this.modeId = modeId;
		this.commandFactory = commandFactory;
	}

	public String getModeId() {
		return modeId;
	}

	public CommandFactory getCommandFactory() {
		return commandFactory;
	}


}
