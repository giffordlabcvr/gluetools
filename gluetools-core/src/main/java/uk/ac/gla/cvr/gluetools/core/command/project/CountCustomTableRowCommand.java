package uk.ac.gla.cvr.gluetools.core.command.project;

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
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"count", "custom-table-row"}, 
		docoptUsages={"<tableName> [-w <whereClause>]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>           Qualify result set"},
		description="Count custom table rows",
		furtherHelp=
		"The optional whereClause qualifies which custom table rows are included.\n"+
		"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
		"Where fieldNames are specified, only these field values will be displayed.\n"+
		"Examples:\n"+
		"  count custom-table-row my_table -w \"id like 'NS%'\"\n"+
		"  count custom-table-row my_table -w \"custom_field = 'value1'\"\n"+
		"  count custom-table-row my_table id custom_field") 
public class CountCustomTableRowCommand extends AbstractCountCTableCommand {

	public static final String TABLE_NAME = "tableName";
	
	private String tableName;
	
	public CountCustomTableRowCommand() {
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
		}
	}

}
