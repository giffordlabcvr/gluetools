package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtable.CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"delete", "custom-table"}, 
		docoptUsages={"<tableName>"},
		description="Delete a custom table",
		metaTags={CmdMeta.updatesDatabase}) 
public class DeleteCustomTableCommand extends ProjectSchemaModeCommand<DeleteResult> {

	public static final String TABLE_NAME = "tableName";

	private String tableName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureIdentifierProperty(configElem, TABLE_NAME, true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		Project project = GlueDataObject.lookup(cmdContext, Project.class, Project.pkMap(getProjectName()));
		CustomTable customTable = GlueDataObject.lookup(cmdContext, CustomTable.class, CustomTable.pkMap(getProjectName(), tableName), false);
		ModelBuilder.deleteCustomTableFromModel(cmdContext.getGluetoolsEngine(), project, customTable);
		GlueDataObject.delete(cmdContext, CustomTable.class, CustomTable.pkMap(getProjectName(), tableName), false);
		cmdContext.commit();
		return new DeleteResult(CustomTable.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("tableName", CustomTable.class, CustomTable.NAME_PROPERTY);
		}
	}


}

