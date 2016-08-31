package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Map;

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
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"create","custom-table-row"}, 
	docoptUsages={"[-a] [-C] <tableName> <rowId>"},
	docoptOptions={"-a, --allowExisting  Allow case where row ID <rowID> exists",
			"-C, --noCommit     Don't commit to the database [default: false]"},
	metaTags={CmdMeta.updatesDatabase},
	description="Create a new row in a custom table") 
public class CreateCustomTableRowCommand extends ProjectModeCommand<CreateResult> {

	public static final String TABLE_NAME = "tableName";
	public static final String ROW_ID = "rowId";
	public static final String ALLOW_EXISTING = "allowExisting";
	public static final String NO_COMMIT = "noCommit";
	
	private String tableName;
	private String rowId;
	private boolean allowExisting;
	private boolean noCommit;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
		rowId = PluginUtils.configureStringProperty(configElem, ROW_ID, true);
		noCommit = PluginUtils.configureBooleanProperty(configElem, NO_COMMIT, true);
		allowExisting = PluginUtils.configureBooleanProperty(configElem, ALLOW_EXISTING, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		CustomTable customTable = getProjectMode(cmdContext).getProject().getCustomTable(tableName);
		Class<? extends CustomTableObject> rowClass = customTable.getRowClass();
		Map<String, String> pkMap = CustomTableObject.pkMap(rowId);
		CustomTableObject existing = null;
		if(allowExisting) {
			existing = GlueDataObject.lookup(cmdContext, rowClass, pkMap, true);
		}
		int numCreated = 0;
		if(existing == null) {
			GlueDataObject.create(cmdContext, rowClass, pkMap, false);
			numCreated = 1;
		}
		if(!noCommit) {
			cmdContext.commit();
		}
		return new CreateResult(rowClass, numCreated);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new AdvancedCmdCompleter.CustomTableNameInstantiator());
		}
	}
	
}
