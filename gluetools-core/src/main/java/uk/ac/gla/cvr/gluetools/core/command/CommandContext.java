package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;


public class CommandContext {
	
	private GluetoolsEngine gluetoolsEngine;
	private List<ObjectContext> objectContextStack = new LinkedList<ObjectContext>();
	
	public CommandContext(GluetoolsEngine gluetoolsEngine) {
		super();
		this.gluetoolsEngine = gluetoolsEngine;
	}

	private List<CommandMode> commandModeStack = new ArrayList<CommandMode>();
	private Optional<CommandContextListener> commandContextListener = Optional.empty();
	
	public void pushCommandMode(CommandMode commandMode) {
		commandMode.setParentCommandMode(peekCommandMode());
		commandModeStack.add(0, commandMode);
		if(commandMode instanceof DbContextChangingMode) {
			ServerRuntime serverRuntime = ((DbContextChangingMode) commandMode).getNewServerRuntime();
			objectContextStack.add(0, GlueDataObject.createObjectContext(serverRuntime));
		}
		commandContextListener.ifPresent(c -> c.commandModeChanged());
	}
	
	public CommandMode popCommandMode() {
		CommandMode commandMode = commandModeStack.remove(0);
		if(commandMode instanceof DbContextChangingMode) {
			objectContextStack.remove(0);
		}
		commandMode.exit();
		commandContextListener.ifPresent(c -> c.commandModeChanged());
		return commandMode;
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
		String path = String.join("", modeIds);
		if(path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	
	
	public GluetoolsEngine getGluetoolsEngine() {
		return gluetoolsEngine;
	}
	
	public Command commandFromElement(Element element) {
		CommandFactory commandFactory = peekCommandMode().getCommandFactory();
		return commandFactory.commandFromElement(this, commandModeStack, gluetoolsEngine.createPluginConfigContext(), element);
	}

	public CommandResult executeElem(Element elem) {
		return commandFromElement(elem).execute(this);
	}

	public ObjectContext getObjectContext() {
		return objectContextStack.get(0);
	}

	public void commit() {
		getObjectContext().commitChanges();
	}
}
