package uk.ac.gla.cvr.gluetools.core.command.root;

import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class RootCommandFactory extends CommandFactory {

	public static Multiton.Creator<RootCommandFactory> creator = new
			Multiton.SuppliedCreator<>(RootCommandFactory.class, RootCommandFactory::new);

	public RootCommandFactory() {
		super();
		registerCommandClass(ProjectCommand.class);
		registerCommandClass(CreateProjectCommand.class);
		registerCommandClass(DeleteProjectCommand.class);
		registerCommandClass(ListProjectsCommand.class);
		
		registerCommandClass(CreateSequenceFieldCommand.class);
		registerCommandClass(DeleteSequenceFieldCommand.class);
		registerCommandClass(ListSequenceFieldsCommand.class);


	}
	

}
