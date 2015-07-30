package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

@CommandModeClass(commandFactoryClass = SequenceModeCommandFactory.class)
public class SequenceMode extends CommandMode implements InsideProjectMode {

	private Project project;
	private String sourceName;
	private String sequenceID;
	
	public SequenceMode(Project project, String sourceName, String sequenceID) {
		super("sequence/"+sourceName+"/"+sequenceID+"/");
		this.project = project;
		this.sourceName = sourceName;
		this.sequenceID = sequenceID;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(SequenceModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "sourceName", sourceName, JsonType.String);
			XmlUtils.appendElementWithText(elem, "sequenceID", sequenceID, JsonType.String);
		}
	}
	
	public Project getProject() {
		return project;
	}
	
}
