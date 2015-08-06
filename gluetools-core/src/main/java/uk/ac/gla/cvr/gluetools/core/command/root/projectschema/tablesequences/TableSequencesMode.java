package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.TableSequencesCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

@CommandModeClass(commandFactoryClass = TableSequencesModeCommandFactory.class)
public class TableSequencesMode extends CommandMode<TableSequencesCommand> {

	
	private Project project;
	
	public TableSequencesMode(CommandContext cmdContext, Project project, TableSequencesCommand command, String sequencesWord) {
		super(command, sequencesWord);
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(TableSequencesModeCommand.class.isAssignableFrom(cmdClass)) {
			GlueXmlUtils.appendElementWithText(elem, "projectName", project.getName(), JsonType.String);
		}
	}

	
}
