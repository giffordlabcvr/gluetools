package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.TableCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = TableSequencesModeCommandFactory.class)
public class TableMode extends CommandMode<TableCommand> {

	
	private Project project;
	private ConfigurableTable cTable;
	
	public TableMode(CommandContext cmdContext, Project project, TableCommand command, ConfigurableTable cTable) {
		super(command, cTable.name());
		this.project = project;
		this.cTable = cTable;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(TableModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, TableModeCommand.PROJECT_NAME, project.getName());
			appendModeConfigToElem(elem, TableModeCommand.TABLE_NAME, cTable.name());
		}
	}

	public Project getProject() {
		return project;
	}

	public ConfigurableTable getConfigurableTable() {
		return cTable;
	}

	
}
