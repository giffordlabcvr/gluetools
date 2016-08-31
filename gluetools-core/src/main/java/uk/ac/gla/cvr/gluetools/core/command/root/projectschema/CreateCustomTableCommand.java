package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"create", "custom-table"}, 
		docoptUsages={"<tableName> [-l <idFieldLength>]"},
		docoptOptions={"-l <idFieldLength>, --idFieldLength <idFieldLength>  Max length of ID"},
		description="Create a new custom table",
		metaTags={CmdMeta.updatesDatabase},
		furtherHelp="The table name must be a valid database identifier, e.g. my_table_1.") 
public class CreateCustomTableCommand extends ProjectSchemaModeCommand<CreateResult> {

	public static final String TABLE_NAME = "tableName";
	public static final String ID_FIELD_LENGTH = "idFieldLength";

	private String tableName;
	private Integer idFieldLength;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureIdentifierProperty(configElem, TABLE_NAME, true);
		idFieldLength = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, ID_FIELD_LENGTH, false)).orElse(50);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {

		CustomTable customTable = GlueDataObject.create(cmdContext, CustomTable.class, CustomTable.pkMap(getProjectName(), tableName), false);
		customTable.setIdFieldLength(idFieldLength);
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(getProjectName()));
		ModelBuilder.addCustomTableToModel(cmdContext.getGluetoolsEngine(), project, customTable);
		customTable.setProject(project);
		cmdContext.commit();
		return new CreateResult(CustomTable.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}


}

