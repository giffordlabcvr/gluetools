package uk.ac.gla.cvr.gluetools.core.command;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

@SuppressWarnings("rawtypes")
public abstract class CommandMode<C extends Command> {
	
	public static String CORE_DOMAIN_RESOURCE = "cayenne-gluecore-domain.xml";
	public static String CORE_MAP_RESOURCE = "gluecore-map.map.xml";
	
	private CommandMode<?> parentCommandMode;

	
	// TODO need to enhance this so that Console commands are only available 
	// for a given mode when we are using the console.
	
	private String relativeModePath;
	
	private CommandFactory commandFactory;

	protected CommandMode(CommandFactory commandFactory, C enterModeCommand, String... modeIds) {
		setRelativeModePath(formRelativeModePath(enterModeCommand, modeIds));
		setCommandFactory(commandFactory);
	}

	protected CommandMode(C enterModeCommand, String... modeIds) {
		setRelativeModePath(formRelativeModePath(enterModeCommand, modeIds));
		setCommandFactory(CommandFactory.get(getClass().getAnnotation(CommandModeClass.class).commandFactoryClass()));
	}
	
	private static String formRelativeModePath(Command enterModeCommand, String... modeIds) {
		if(enterModeCommand == null) {
			return "/";
		}
		String firstCommandWord = CommandUsage.cmdWordsForCmdClass(enterModeCommand.getClass())[0];
		if(modeIds.length == 0) {
			return firstCommandWord+"/";
		}
		return firstCommandWord+"/"+String.join("/", modeIds)+"/";
	}
	
	public String getRelativeModePath() {
		return relativeModePath;
	}

	public CommandFactory getCommandFactory() {
		return commandFactory;
	}

	private void setRelativeModePath(String modeId) {
		this.relativeModePath = modeId;
	}

	private void setCommandFactory(CommandFactory commandFactory) {
		this.commandFactory = commandFactory;
	}


	@SuppressWarnings("rawtypes")
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass, Element elem) {
		
	}

	public ServerRuntime getServerRuntime() {
		CommandMode<?> parentCommandMode = getParentCommandMode();
		if(parentCommandMode != null) {
			return parentCommandMode.getServerRuntime();
		}
		return null;
	}

	protected CommandMode<?> getParentCommandMode() {
		return parentCommandMode;
	}

	void setParentCommandMode(CommandMode<?> parentCommandMode) {
		this.parentCommandMode = parentCommandMode;
	}

	public void exit() {
	}
	
	protected void appendModeConfigToElem(Element elem, String name, Object value) {
		if(value == null) {
			Element newElem = GlueXmlUtils.appendElement(elem, name);
			JsonUtils.setJsonType(newElem, JsonType.Null, false);
		} else {
			Element newElem = (Element) GlueXmlUtils.appendElementWithText(elem, name, value.toString()).getParentNode();
			JsonUtils.setJsonType(newElem, JsonUtils.jsonTypeFromObject(value), false);
		}
	}


	
}
