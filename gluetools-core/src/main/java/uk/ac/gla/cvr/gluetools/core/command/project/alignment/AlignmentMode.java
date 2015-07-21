package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class AlignmentMode extends CommandMode {

	
	private String alignmentName;
	
	public AlignmentMode(CommandContext cmdContext, String alignmentName) {
		super("alnmt-"+alignmentName, new AlignmentModeCommandFactory());
		this.alignmentName = alignmentName;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(AlignmentModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "alignmentName", alignmentName);
		}
	}

	public String getAlignmentName() {
		return alignmentName;
	}

	
}
