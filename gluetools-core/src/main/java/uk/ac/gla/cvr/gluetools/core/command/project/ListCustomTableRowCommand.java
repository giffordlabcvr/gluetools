package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"list", "custom-table-row"}, 
		docoptUsages={"<tableName> [-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [<fieldName> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>           Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>     Limit max number of records",
		"-o <fetchOffset>, --fetchOffset <fetchOffset>  Record number offset"},
		description="List alignments",
		furtherHelp=
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional whereClause qualifies which custom table rows are displayed.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  list custom-table-row my_table -w \"id like 'NS%'\"\n"+
		"  list custom-table-row my_table -w \"custom_field = 'value1'\"\n"+
		"  list custom-table-row my_table id custom_field") 
public class ListCustomTableRowCommand extends AbstractListCTableCommand {

	public static final String TABLE_NAME = "tableName";
	
	private String tableName;
	
	public ListCustomTableRowCommand() {
		super();
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
		setTableName(tableName);
	}


	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
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
			registerVariableInstantiator("fieldName", new AdvancedCmdCompleter.VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
					Project project = insideProjectMode.getProject();
					CustomTable customTable = project.getCustomTable((String) bindings.get("tableName")); 
					if(customTable != null) {
						return project.getListableProperties(customTable.getName())
							.stream()
							.map(p -> new CompletionSuggestion(p, true))
							.collect(Collectors.toList());
					}
					return new ArrayList<CompletionSuggestion>();
				}
			});
		}
	}

}
