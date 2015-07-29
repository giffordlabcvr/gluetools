package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.DbContextChangingMode;

public class RootCommandMode extends CommandMode implements DbContextChangingMode {

	private ServerRuntime newServerRuntime;
	
	public RootCommandMode(ServerRuntime serverRuntime) {
		super("GLUE", CommandFactory.get(RootCommandFactory.creator));
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
