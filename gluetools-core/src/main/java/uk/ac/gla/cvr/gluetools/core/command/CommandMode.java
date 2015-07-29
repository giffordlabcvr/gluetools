package uk.ac.gla.cvr.gluetools.core.command;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;


public abstract class CommandMode {
	
	public static String CORE_DOMAIN_RESOURCE = "cayenne-gluecore-domain.xml";
	public static String CORE_MAP_RESOURCE = "gluecore-map.map.xml";
	
	private CommandMode parentCommandMode;

	
	// TODO need to enhance this so that Console commands are only available 
	// for a given mode when we are using the console.
	
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

	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass, Element elem) {
		
	}

	public ServerRuntime getServerRuntime() {
		CommandMode parentCommandMode = getParentCommandMode();
		if(parentCommandMode != null) {
			return parentCommandMode.getServerRuntime();
		}
		return null;
	}

	protected CommandMode getParentCommandMode() {
		return parentCommandMode;
	}

	void setParentCommandMode(CommandMode parentCommandMode) {
		this.parentCommandMode = parentCommandMode;
	}

	public void exit() {
	}
	
}
