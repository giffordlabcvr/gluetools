package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.MemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = MemberModeCommandFactory.class)
public class MemberMode extends CommandMode<MemberCommand> implements InsideProjectMode {

	
	private Project project;
	private String sourceName;
	private String sequenceID;
	
	public MemberMode(Project project, MemberCommand command, String sourceName, String sequenceID) {
		super(command, sourceName, sequenceID);
		this.project = project;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(MemberModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "sourceName", sourceName);
			appendModeConfigToElem(elem, "sequenceID", sequenceID);
		}
	}
	
	

	public Project getProject() {
		return project;
	}

	
}
