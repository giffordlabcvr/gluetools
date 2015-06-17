package uk.ac.gla.cvr.gluetools.core.command.root;

import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class RootCommandFactory extends CommandFactory {

	public static Multiton.Creator<RootCommandFactory> creator = new
			Multiton.SuppliedCreator<>(RootCommandFactory.class, RootCommandFactory::new);

	public RootCommandFactory() {
		super();
		registerPluginClass(ProjectCommand.class);
		registerPluginClass(CreateProjectCommand.class);
		registerPluginClass(DeleteProjectCommand.class);
		registerPluginClass(ListProjectsCommand.class);
	}
	

}
