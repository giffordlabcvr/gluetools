package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import java.util.Arrays;
import java.util.stream.Collectors;

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
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
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

	private String cTableName;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.cTableName = PluginUtils.configureStringProperty(configElem, "cTable", true);
		if(cTableName.equals("SEQUENCES")) {
			GlueLogger.getGlueLogger().warning("Command \"table SEQUENCES\" is deprecated in favour of \"table sequence\" please update your GLUE scripts accordingly.");
			this.cTableName = "sequence";
		}
		if(cTableName.equals("SEQUENCE")) {
			GlueLogger.getGlueLogger().warning("Command \"table SEQUENCE\" is deprecated in favour of \"table sequence\" please update your GLUE scripts accordingly.");
			this.cTableName = "sequence";
		}
		try {
			Enum.valueOf(ModelBuilder.ConfigurableTable.class, cTableName);
		} catch(IllegalArgumentException iae) {
			throw new ProjectSchemaException(ProjectSchemaException.Code.NOT_A_CONFIGURABLE_TABLE, cTableName);
		}
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Project project = getProjectSchemaMode(cmdContext).getProject();
		cmdContext.pushCommandMode(new TableMode(cmdContext, project, this, cTableName));
		return CommandResult.OK;
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerStringListLookup("cTable", 
					Arrays.asList(ModelBuilder.ConfigurableTable.values())
					.stream()
					.map(ct -> ct.name()).collect(Collectors.toList()));
		}
		
	}
	
}
