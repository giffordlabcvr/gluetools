package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class TableModeCommand<R extends CommandResult> extends Command<R> {

	public static final String PROJECT_NAME = "projectName";
	public static final String TABLE_NAME = "tableName";

	private String projectName;
	private String tableName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		projectName = PluginUtils.configureStringProperty(configElem, PROJECT_NAME, true);
		tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
	}

	protected String getProjectName() {
		return projectName;
	}

	protected String getTableName() {
		return tableName;
	}

}
