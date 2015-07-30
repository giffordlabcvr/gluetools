package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class ProjectSchemaMode extends CommandMode {

	
	private Project project;
	
	public ProjectSchemaMode(CommandContext cmdContext, Project project) {
		super("schema-project/"+project.getName()+"/", CommandFactory.get(ProjectSchemaModeCommandFactory.creator));
		this.project = project;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(ProjectSchemaModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "projectName", project.getName(), JsonType.String);
		}
	}

	public Project getProject() {
		return project;
	}

	
}
