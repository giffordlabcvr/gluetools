package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table.TableMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"table"},
	docoptUsages={"<cTable>"},
	description="Mode to manage the custom fields of a configurable table", 
	furtherHelp="Supported table names: "+ModelBuilder.configurableTablesString+
	". Other table customization may become available in the future.")
@EnterModeCommandClass(
		commandModeClass = TableMode.class)
public class TableCommand extends ProjectSchemaModeCommand<OkResult>  {

	private ConfigurableTable cTable;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.cTable = PluginUtils.configureEnumProperty(ConfigurableTable.class, configElem, "cTable", true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Project project = getProjectSchemaMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new TableMode(cmdContext, project, this, cTable));
		return CommandResult.OK;
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("cTable", ConfigurableTable.class);
		}
		
	}
	
}
