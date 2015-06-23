package uk.ac.gla.cvr.gluetools.core.command;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;


public abstract class CommandMode {
	
	public static String CAYENNE_DOMAIN_RESOURCE = "cayenne-gluecore-domain.xml";
	public static String CAYENNE_MAP_RESOURCE = "gluecore-map.map.xml";
	
	private ServerRuntime cayenneServerRuntime;
	private CommandMode parentCommandMode;

	
	// TODO need to enhance this so that Console commands are only available 
	// for a given mode when we are using the console.
	public static CommandMode ROOT = new RootCommandMode();
	
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

	public ServerRuntime getCayenneServerRuntime() {
		if(cayenneServerRuntime == null) {
			CommandMode parentCommandMode = getParentCommandMode();
			if(parentCommandMode != null) {
				return parentCommandMode.getCayenneServerRuntime();
			}
		}
		return cayenneServerRuntime;
	}

	protected void setCayenneServerRuntime(ServerRuntime cayenneServerRuntime) {
		this.cayenneServerRuntime = cayenneServerRuntime;
	}

	protected CommandMode getParentCommandMode() {
		return parentCommandMode;
	}

	void setParentCommandMode(CommandMode parentCommandMode) {
		this.parentCommandMode = parentCommandMode;
	}

	void exit() {
		if(cayenneServerRuntime != null) {
			cayenneServerRuntime.shutdown();
		}
	}
	
}
