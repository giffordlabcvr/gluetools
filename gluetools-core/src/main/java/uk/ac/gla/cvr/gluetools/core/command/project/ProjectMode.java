package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public class ProjectMode extends CommandMode {

	
	private Project project;
	
	public ProjectMode(CommandContext cmdContext, Project project) {
		super("project-"+project.getName(), PluginFactory.get(ProjectModeCommandFactory.creator));
		this.project = project;
		setServerRuntime(ModelBuilder.createProjectModel(getServerRuntime(), project));
	}

	public Field getSequenceField(String fieldName) {
		return project.getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().get();
	}

	public List<String> getSequenceFieldNames() {
		return project.getFields().stream().map(Field::getName).collect(Collectors.toList());
	}
	
	
	
}
