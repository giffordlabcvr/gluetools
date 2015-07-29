package uk.ac.gla.cvr.gluetools.core.command;

import org.apache.cayenne.configuration.server.ServerRuntime;

public interface DbContextChangingMode {

	public ServerRuntime getNewServerRuntime();

	public void setNewServerRuntime(ServerRuntime serverRuntime);

}
