package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.configuration.server.ServerRuntime;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.DbContextChangingMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public class ProjectMode extends CommandMode implements InsideProjectMode, DbContextChangingMode {

	
	private Project project;
	private ServerRuntime newServerRuntime;
	
	public ProjectMode(CommandContext cmdContext, Project project) {
		super("proj-"+project.getName(), CommandFactory.get(ProjectModeCommandFactory.creator));
		this.project = project;
		setNewServerRuntime(ModelBuilder.createProjectModel(cmdContext.getGluetoolsEngine().getDbConfiguration(), project));
	}
	
	public Project getProject() {
		return project;
	}

	public ServerRuntime getNewServerRuntime() {
		return newServerRuntime;
	}

	public void setNewServerRuntime(ServerRuntime newServerRuntime) {
		this.newServerRuntime = newServerRuntime;
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
