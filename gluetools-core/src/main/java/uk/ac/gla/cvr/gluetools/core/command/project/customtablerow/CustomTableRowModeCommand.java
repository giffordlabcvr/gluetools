package uk.ac.gla.cvr.gluetools.core.command.project.customtablerow;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class CustomTableRowModeCommand<R extends CommandResult> extends ProjectModeCommand<R> {


	private String tableName;
	private String rowId;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureStringProperty(configElem, "tableName", true);
		rowId = PluginUtils.configureStringProperty(configElem, "rowId", true);
	}

	protected String getTableName() {
		return tableName;
	}

	protected String getRowId() {
		return rowId;
	}


	protected static CustomTableRowMode getCustomTableRowMode(CommandContext cmdContext) {
		return (CustomTableRowMode) cmdContext.peekCommandMode();
	}


	protected CustomTableObject lookupCustomTableRow(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, getCustomTableRowMode(cmdContext).getCustomTable().getRowClass(), CustomTableObject.pkMap(rowId));
	}

	

}
