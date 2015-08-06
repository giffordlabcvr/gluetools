package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.command.root.ProjectSchemaCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = ProjectSchemaModeCommandFactory.class)
public class ProjectSchemaMode extends CommandMode<ProjectSchemaCommand> {

	
	private Project project;
	
	public ProjectSchemaMode(CommandContext cmdContext, ProjectSchemaCommand command, Project project) {
		super(command, project.getName());
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(ProjectSchemaModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "projectName", project.getName());
		}
	}

	public Project getProject() {
		return project;
	}

	
}
