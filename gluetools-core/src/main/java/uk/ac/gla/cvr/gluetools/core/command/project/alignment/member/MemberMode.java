package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class MemberMode extends CommandMode implements InsideProjectMode {

	
	private Project project;
	private String sourceName;
	private String sequenceID;
	
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(MemberModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "sourceName", sourceName, JsonType.String);
			XmlUtils.appendElementWithText(elem, "sequenceID", sequenceID, JsonType.String);
		}
	}
	
	public MemberMode(Project project, String sourceName, String sequenceID) {
		super("memb-"+sourceName+"-"+sequenceID, CommandFactory.get(MemberModeCommandFactory.creator));
		this.project = project;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
	}

	public Project getProject() {
		return project;
	}

	
}
