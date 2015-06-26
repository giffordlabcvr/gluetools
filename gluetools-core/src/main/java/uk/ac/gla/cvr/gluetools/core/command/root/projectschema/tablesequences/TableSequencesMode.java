package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class TableSequencesMode extends CommandMode {

	
	private Project project;
	
	public TableSequencesMode(CommandContext cmdContext, Project project) {
		super("table-SEQUENCES", CommandFactory.get(TableSequencesModeCommandFactory.creator));
		this.project = project;
	}

	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(TableSequencesModeCommand.class.isAssignableFrom(cmdClass)) {
			XmlUtils.appendElementWithText(elem, "projectName", project.getName());
		}
	}

	
}
