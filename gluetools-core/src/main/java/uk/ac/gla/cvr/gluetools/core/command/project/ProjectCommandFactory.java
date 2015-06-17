package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ProjectCommandFactory extends CommandFactory {

	public static Multiton.Creator<ProjectCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ProjectCommandFactory.class, ProjectCommandFactory::new);

	public ProjectCommandFactory() {
		super();
	}
	

}
