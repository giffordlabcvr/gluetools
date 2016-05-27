package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "field"}, 
	docoptUsages={"<fieldName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a field from the table") 
public class DeleteFieldCommand extends TableModeCommand<DeleteResult> {

	private String fieldName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, "fieldName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		Map<String, String> pkMap = Field.pkMap(getProjectName(), getTableName(), fieldName);
		Field field = GlueDataObject.lookup(cmdContext, Field.class, pkMap);
		ModelBuilder.deleteTableColumnFromModel(cmdContext.getGluetoolsEngine(), field.getProject(), field);
		DeleteResult result = GlueDataObject.delete(cmdContext, Field.class, pkMap, true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("fieldName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					TableMode tableMode = (TableMode) cmdContext.peekCommandMode();
					String projectName = tableMode.getProject().getName();
					List<Field> fields = GlueDataObject.query(cmdContext, Field.class, 
							new SelectQuery(Field.class, ExpressionFactory.matchExp(Field.PROJECT_PROPERTY, projectName)
									.andExp(ExpressionFactory.matchExp(Field.TABLE_PROPERTY, tableMode.getConfigurableTable().name()))));
					return fields.stream().map(f -> new CompletionSuggestion(f.getName(), true)).collect(Collectors.toList());
				}
			});
		}
	}

}
