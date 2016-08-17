package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Collections;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"multi-unset", "field"}, 
		docoptUsages={"<tableName> (-w <whereClause> | -a) <fieldName>  [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated objects", 
				"-a, --allObjects                               Update all objects in table",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Unset a field's value for one or more sequences", 
		furtherHelp="The <cTable> argument specifies a configurable object table. "+
				" Possible values are "+ModelBuilder.configurableTablesString+
				". Unsetting means reverting the field value to null."+
				" Updates to the database are committed in batches, the default batch size is 250.") 
public class MultiUnsetFieldCommand extends MultiFieldUpdateCommand {

	public static final String FIELD_NAME = "fieldName";

	private String fieldName;

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
	}
	
	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		Project project = getProjectMode(cmdContext).getProject();
		project.checkModifiableFieldNames(getTableName(), Collections.singletonList(fieldName));
		return executeUpdates(cmdContext);
	}

	@Override
	protected void updateObject(CommandContext cmdContext, GlueDataObject object) {
		object.writeProperty(fieldName, null);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new MultiFieldUpdateCommand.TableNameInstantiator());
			registerVariableInstantiator("fieldName", new ModifiableFieldInstantiator());
		}
		
	}

}
