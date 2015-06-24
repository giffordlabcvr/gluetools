package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;


public class CommandContext {
	
	private GluetoolsEngine gluetoolsEngine;
	private ObjectContext objectContext;
	
	public CommandContext(GluetoolsEngine gluetoolsEngine) {
		super();
		this.gluetoolsEngine = gluetoolsEngine;
	}

	private List<CommandMode> commandModeStack = new ArrayList<CommandMode>();
	private Optional<CommandContextListener> commandContextListener;
	
	public void pushCommandMode(CommandMode commandMode) {
		commandMode.setParentCommandMode(peekCommandMode());
		commandModeStack.add(0, commandMode);
		commandContextListener.ifPresent(c -> c.commandModeChanged());
	}
	
	public void popCommandMode() {
		CommandMode commandMode = commandModeStack.remove(0);
		commandMode.exit();
		commandContextListener.ifPresent(c -> c.commandModeChanged());
	}
	
	public CommandMode peekCommandMode() {
		if(commandModeStack.isEmpty()) {
			return null;
		}
		return commandModeStack.get(0);
	}
	
	public void setCommandContextListener(CommandContextListener listener) {
		commandContextListener = Optional.ofNullable(listener);
	}

	public String getModePath() {
		List<String> modeIds = 
				commandModeStack.stream().
				map(mode -> mode.getModeId()).collect(Collectors.toList());
		Collections.reverse(modeIds);
		return String.join("/", modeIds);
	}

	
	
	public GluetoolsEngine getGluetoolsEngine() {
		return gluetoolsEngine;
	}
	
	public Command commandFromElement(Element element) {
		CommandFactory commandFactory = peekCommandMode().getCommandFactory();
		return commandFactory.commandFromElement(commandModeStack, gluetoolsEngine.createPluginConfigContext(), element);
	}

	public CommandResult executeElem(Element elem) {
		return commandFromElement(elem).execute(this);
	}

	public ObjectContext getObjectContext() {
		return objectContext;
	}

	/**
	 * commands should not use this.
	 */
	public void setObjectContext(ObjectContext objectContext) {
		this.objectContext = objectContext;
	}
	
	
}
