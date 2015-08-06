package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.DbContextChangingMode;

@CommandModeClass(commandFactoryClass = RootCommandFactory.class)
@SuppressWarnings("rawtypes")
public class RootCommandMode extends CommandMode<Command> implements DbContextChangingMode {

	private ServerRuntime newServerRuntime;
	
	public RootCommandMode(ServerRuntime serverRuntime) {
		super(null);
		setNewServerRuntime(serverRuntime);
	}

	@Override
	public ServerRuntime getNewServerRuntime() {
		return newServerRuntime;
	}

	@Override
	public void setNewServerRuntime(ServerRuntime serverRuntime) {
		this.newServerRuntime = serverRuntime;
	}

	@Override
	public ServerRuntime getServerRuntime() {
		return newServerRuntime;
	}
	
	@Override
	public void exit() {
		newServerRuntime.shutdown();
	}
	
	
}
