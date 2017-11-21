package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

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
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table.TableMode;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table.TableModeCommandFactory;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"table"},
	docoptUsages={"<tableName>"},
	description="Mode to manage the custom fields of a configurable table")
@EnterModeCommandClass(
		commandFactoryClass = TableModeCommandFactory.class)
public class TableCommand extends ProjectSchemaModeCommand<OkResult>  {

	private String tableName;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.tableName = PluginUtils.configureStringProperty(configElem, "tableName", true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Project project = getProjectSchemaMode(cmdContext).getProject();
		project.checkTableName(tableName);
		cmdContext.pushCommandMode(new TableMode(cmdContext, project, this, tableName));
		return CommandResult.OK;
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new VariableInstantiator() {
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					InsideProjectMode insideProjectMode = (ProjectSchemaMode) cmdContext.peekCommandMode();
					return insideProjectMode.getProject().getTableNames()
							.stream()
							.map(n -> new CompletionSuggestion(n, true))
							.collect(Collectors.toList());
				}
			});
		}
		
	}
	
}
