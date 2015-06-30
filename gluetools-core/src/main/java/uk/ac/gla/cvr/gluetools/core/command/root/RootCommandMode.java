package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;

// TODO domain resource to be configured by engine config properties.
public class RootCommandMode extends CommandMode {

	public RootCommandMode(ServerRuntime serverRuntime) {
		super("GLUE", CommandFactory.get(RootCommandFactory.creator));
		setServerRuntime(serverRuntime);
	}

}
