package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.project.AlignmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

@CommandModeClass(commandFactoryClass = AlignmentModeCommandFactory.class)
public class AlignmentMode extends CommandMode<AlignmentCommand> implements InsideProjectMode {
	
	private String alignmentName;
	private Project project;
	
	public AlignmentMode(Project project, AlignmentCommand command, String alignmentName) {
		super(command, alignmentName);
		this.alignmentName = alignmentName;
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(AlignmentModeCommand.class.isAssignableFrom(cmdClass)) {
			GlueXmlUtils.appendElementWithText(elem, "alignmentName", alignmentName, JsonType.String);
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
