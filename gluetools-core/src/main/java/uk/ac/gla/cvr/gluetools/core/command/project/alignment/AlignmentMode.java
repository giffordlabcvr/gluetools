package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

@CommandModeClass(commandFactoryClass = AlignmentModeCommandFactory.class)
public class AlignmentMode extends CommandMode implements InsideProjectMode {
	
	private String alignmentName;
	private Project project;
	
	public AlignmentMode(Project project, String alignmentName) {
		super("alignment/"+alignmentName+"/");
		this.alignmentName = alignmentName;
		this.project = project;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(AlignmentModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "alignmentName", alignmentName, JsonType.String);
		}
	}

	public String getAlignmentName() {
		return alignmentName;
	}

	@Override
	public Project getProject() {
		return project;
	}

	
}
