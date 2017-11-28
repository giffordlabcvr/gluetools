package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.customtablerow.CustomTableRowMode;
import uk.ac.gla.cvr.gluetools.core.command.project.customtablerow.CustomTableRowModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"custom-table-row"},
	docoptUsages={"<tableName> <rowId>"},
	description="Enter command mode for a custom table row")
@EnterModeCommandClass(
		commandFactoryClass = CustomTableRowModeCommandFactory.class, 
		modeDescription = "Custom object mode")
public class CustomTableRowCommand extends ProjectModeCommand<OkResult>  {

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
	public OkResult execute(CommandContext cmdContext) {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		Project project = insideProjectMode.getProject();
		CustomTable customTable = project.getCustomTable(tableName); 
		if(customTable == null) {
			throw new ProjectModeCommandException(Code.NO_SUCH_TABLE, tableName);
		}
		// check row exists.
		GlueDataObject.lookup(cmdContext, customTable.getRowClass(), CustomTableObject.pkMap(rowId));
		cmdContext.pushCommandMode(new CustomTableRowMode(project, this, customTable, rowId));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new AdvancedCmdCompleter.VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
					return insideProjectMode.getProject().getCustomTables()
						.stream().map(t -> new CompletionSuggestion(t.getName(), true)).collect(Collectors.toList());
				}
			});
			registerVariableInstantiator("rowId", new AdvancedCmdCompleter.VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
					CustomTable customTable = insideProjectMode.getProject().getCustomTable((String) bindings.get("tableName")); 
					if(customTable != null) {
						return listNames(cmdContext, prefix, customTable.getRowClass(), CustomTableObject.ID_PROPERTY);
					}
					return new ArrayList<CompletionSuggestion>();
				}
			});
		}
		
	}	

}
