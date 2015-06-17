package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public class ProjectCommandMode extends CommandMode {

	private String projectId;
	
	public ProjectCommandMode(String projectId) {
		super("project-"+projectId, PluginFactory.get(ProjectCommandFactory.creator));
		this.projectId = projectId;
	}

}
