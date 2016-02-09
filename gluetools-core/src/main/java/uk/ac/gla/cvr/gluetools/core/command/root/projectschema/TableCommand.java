package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import java.util.Arrays;
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
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table.TableMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"table"},
	docoptUsages={"<tableName>"},
	description="Mode to manage the custom fields of a project table", 
	furtherHelp="Supported table names: SEQUENCE. Other table customization may become available in the future.")
@EnterModeCommandClass(
		commandModeClass = TableMode.class)
public class TableCommand extends ProjectSchemaModeCommand<OkResult>  {

	private String tableName;
	
	public static final List<String> configurableTables = Arrays.asList("SEQUENCE");
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.tableName = PluginUtils.configureStringProperty(configElem, "tableName", true);
		if(tableName.equals("SEQUENCES")) {
			GlueLogger.getGlueLogger().warning("Command \"table SEQUENCES\" is deprecated in favour of \"table SEQUENCE\" please update your GLUE scripts accordingly.");
			this.tableName = "SEQUENCE";
		}
		if(!configurableTables.contains(tableName)) {
			throw new ProjectSchemaException(ProjectSchemaException.Code.NOT_A_CONFIGURABLE_TABLE, tableName);
		}
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Project project = getProjectSchemaMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new TableMode(cmdContext, project, this, tableName));
		return CommandResult.OK;
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new VariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					return configurableTables.stream()
							.map(tName -> new CompletionSuggestion(tName, true)).collect(Collectors.toList());
				}
			});
		}
		
	}
	
}
