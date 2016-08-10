package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","custom-table-row"}, 
	docoptUsages={"<tableName> <rowId>"},
	docoptOptions={},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new row in a custom table") 
public class CreateCustomTableRowCommand extends ProjectModeCommand<CreateResult> {

	public static final String TABLE_NAME = "tableName";
	public static final String ROW_ID = "rowId";
	
	private String tableName;
	private String rowId;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
		rowId = PluginUtils.configureStringProperty(configElem, ROW_ID, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		CustomTable customTable = getProjectMode(cmdContext).getProject().getCustomTable(tableName);
		Class<? extends CustomTableObject> rowClass = customTable.getRowClass();
		GlueDataObject.create(cmdContext, rowClass, CustomTableObject.pkMap(rowId), false);
		cmdContext.commit();
		return new CreateResult(rowClass, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new AdvancedCmdCompleter.CustomTableNameInstantiator());
		}
	}
	
}
