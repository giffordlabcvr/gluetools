package uk.ac.gla.cvr.gluetools.core.command.project.sequenceGroup;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.GroupCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = GroupModeCommandFactory.class)
public class GroupMode extends CommandMode<GroupCommand> implements InsideProjectMode {
	
	private String groupName;
	private Project project;
	
	public GroupMode(Project project, GroupCommand command, String groupName) {
		super(command, groupName);
		this.groupName = groupName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(GroupModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "groupName", groupName);
		}
	}

	public String getGroupName() {
		return groupName;
	}

	@Override
	public Project getProject() {
		return project;
	}


	
}
