package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public class ProjectMode extends CommandMode {

	private String projectName;
	
	public ProjectMode(String projectName) {
		super("project-"+projectName, PluginFactory.get(ProjectModeCommandFactory.creator));
		this.projectName = projectName;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass, Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(ProjectModeCommand.class.isAssignableFrom(cmdClass)) {
			Element projectNameElem = (Element) elem.appendChild(elem.getOwnerDocument().createElement("projectName"));
			projectNameElem.appendChild(elem.getOwnerDocument().createTextNode(projectName));
		}
	}

	
}
