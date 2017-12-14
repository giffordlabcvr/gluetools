package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete","custom-table-row"}, 
	docoptUsages={"<tableName> <rowId>"},
	docoptOptions={},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a row from a custom table") 
public class DeleteCustomTableRowCommand extends ProjectModeCommand<DeleteResult> {

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
	public DeleteResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		project.checkCustomTableName(tableName);
		CustomTable customTable = project.getCustomTable(tableName);
		Class<? extends CustomTableObject> rowClass = customTable.getRowClass();
		DeleteResult delResult = GlueDataObject.delete(cmdContext, rowClass, CustomTableObject.pkMap(rowId), false);
		cmdContext.commit();
		return delResult;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new AdvancedCmdCompleter.CustomTableNameInstantiator());
			registerVariableInstantiator("rowId", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					String tableName = (String) bindings.get("tableName");
					if(tableName != null) {
						CustomTable customTable = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject().getCustomTable(tableName);
						if(customTable != null) {
							return AdvancedCmdCompleter.listNames(cmdContext, prefix, customTable.getRowClass(), CustomTableObject.ID_PROPERTY);
						}
					}
					return new ArrayList<CompletionSuggestion>();
				}
			});
		}
	}
	
}
