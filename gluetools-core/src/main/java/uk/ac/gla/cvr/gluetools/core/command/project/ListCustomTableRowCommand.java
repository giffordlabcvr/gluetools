/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"list", "custom-table-row"}, 
		docoptUsages={"<tableName> [-w <whereClause>] [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [-s <sortProperties>] [<fieldName> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>           Qualify result set",
				"-p <pageSize>, --pageSize <pageSize>                    Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>              Limit max number of records",
				"-o <fetchOffset>, --fetchOffset <fetchOffset>           Record number offset",
				"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties"},
		description="List custom table rows",
		furtherHelp=
		"The <pageSize> option is for performance tuning. The default page size\n"+
		"is 250 records.\n"+
		"The optional whereClause qualifies which custom table rows are displayed.\n"+
		"The optional sortProperties allows combined ascending/descending orderings, e.g. +property1,-property2.\n"+
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


	@Override
	public ListResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		project.checkCustomTableName(tableName);
		return super.execute(cmdContext);
	}


	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			registerVariableInstantiator("tableName", new AdvancedCmdCompleter.VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				public List<CompletionSuggestion> instantiate(
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
				public List<CompletionSuggestion> instantiate(
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
