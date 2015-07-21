package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public class ProjectMode extends CommandMode implements InsideProjectMode {

	
	private Project project;
	
	public ProjectMode(CommandContext cmdContext, Project project) {
		super("proj-"+project.getName(), CommandFactory.get(ProjectModeCommandFactory.creator));
		this.project = project;
		setServerRuntime(ModelBuilder.createProjectModel(cmdContext.getGluetoolsEngine().getDbConfiguration(), project));
	}

	
	public Project getProject() {
		return project;
	}
}
