package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class SequenceMode extends CommandMode {

	private Project project;
	private String sourceName;
	private String sequenceID;
	
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(SequenceModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "sourceName", sourceName);
			XmlUtils.appendElementWithText(elem, "sequenceID", sequenceID);
		}
	}
	
	public SequenceMode(Project project, String sourceName, String sequenceID) {
		super("seq-"+sourceName+"-"+sequenceID, CommandFactory.get(SequenceModeCommandFactory.creator));
		this.project = project;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
	}

	public Project getProject() {
		return project;
	}
	
}
