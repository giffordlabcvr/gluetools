package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.TableCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = TableSequencesModeCommandFactory.class)
public class TableMode extends CommandMode<TableCommand> {

	
	private Project project;
	
	public TableMode(CommandContext cmdContext, Project project, TableCommand command, String tableName) {
		super(command, tableName);
		this.project = project;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(TableModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "projectName", project.getName());
		}
	}

	public Project getProject() {
		return project;
	}

	
}
