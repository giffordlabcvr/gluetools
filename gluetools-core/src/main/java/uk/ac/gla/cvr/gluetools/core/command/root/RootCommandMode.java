package uk.ac.gla.cvr.gluetools.core.command.root;

import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

// TODO domain resource to be configured by engine config properties.
public class RootCommandMode extends CommandMode {

	public RootCommandMode() {
		super("GLUE", PluginFactory.get(RootCommandFactory.creator));
		setCayenneServerRuntime(new ServerRuntime(CORE_DOMAIN_RESOURCE));
	}

}
