package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class ReferenceSequenceMode extends CommandMode implements InsideProjectMode {

	
	private String refSeqName;
	private Project project;
	
	public ReferenceSequenceMode(Project project, String refSeqName) {
		super("ref-"+refSeqName, new ReferenceSequenceModeCommandFactory());
		this.refSeqName = refSeqName;
		this.project = project;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(ReferenceSequenceModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "refSeqName", refSeqName, JsonType.String);
		}
	}

	public String getRefSeqName() {
		return refSeqName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	
}
