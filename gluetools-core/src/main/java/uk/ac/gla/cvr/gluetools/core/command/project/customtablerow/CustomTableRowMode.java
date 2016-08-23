package uk.ac.gla.cvr.gluetools.core.command.project.customtablerow;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.project.CustomTableRowCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.RenderableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.root.CommandModeClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@CommandModeClass(commandFactoryClass = CustomTableRowModeCommandFactory.class)
public class CustomTableRowMode extends CommandMode<CustomTableRowCommand> implements ConfigurableObjectMode, RenderableObjectMode {

	
	private CustomTable customTable;
	private String rowId;
	private Project project;
	
	public CustomTableRowMode(Project project, CustomTableRowCommand command, CustomTable customTable, String rowId) {
		super(command, customTable.getName(), rowId);
		this.project = project;
		this.customTable = customTable;
		this.rowId = rowId;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void addModeConfigToCommandElem(Class<? extends Command> cmdClass,
			Element elem) {
		super.addModeConfigToCommandElem(cmdClass, elem);
		if(CustomTableRowModeCommand.class.isAssignableFrom(cmdClass)) {
			appendModeConfigToElem(elem, "tableName", customTable.getName());
			appendModeConfigToElem(elem, "rowId", rowId);
		}
	}

	public CustomTable getCustomTable() {
		return customTable;
	}

	public String getRowId() {
		return rowId;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public String getTableName() {
		return getCustomTable().getName();
	}

	@Override
	public GlueDataObject getConfigurableObject(CommandContext cmdContext) {
		return lookupCustomTableRow(cmdContext);
	}

	protected CustomTableObject lookupCustomTableRow(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, customTable.getRowClass(), CustomTableObject.pkMap(rowId));
	}

	@Override
	public GlueDataObject getRenderableObject(CommandContext cmdContext) {
		return lookupCustomTableRow(cmdContext);
	}

	
}
