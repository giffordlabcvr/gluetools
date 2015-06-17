package uk.ac.gla.cvr.gluetools.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;


public class CommandContext {
	
	private GluetoolsEngine gluetoolsEngine;
	
	public CommandContext(GluetoolsEngine gluetoolsEngine) {
		super();
		this.gluetoolsEngine = gluetoolsEngine;
	}

	private List<CommandMode> commandModeStack = new ArrayList<CommandMode>();
	private Optional<CommandContextListener> commandContextListener;
	
	public void pushCommandMode(CommandMode commandMode) {
		commandModeStack.add(0, commandMode);
		commandContextListener.ifPresent(c -> c.commandModeChanged());
	}
	
	public void popCommandMode() {
		commandModeStack.remove(0);
		commandContextListener.ifPresent(c -> c.commandModeChanged());
	}
	
	public CommandMode peekCommandMode() {
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
	
}
