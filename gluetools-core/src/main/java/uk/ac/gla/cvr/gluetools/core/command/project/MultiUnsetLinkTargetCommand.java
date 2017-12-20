package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.LinkUpdateContext;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.PropertyCommandDelegate;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass( 
		commandWords={"multi-unset", "link-target"}, 
		docoptUsages={"<tableName> (-w <whereClause> | -a) <linkName> [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated objects", 
				"-a, --allObjects                               Update all objects in table",
				"-b <batchSize>, --batchSize <batchSize>        Update batch size" },
		description="Unset a field's value for one or more sequences", 
		furtherHelp="The <tableName> argument specifies a configurable object table. "+
				" Possible values are "+ModelBuilder.configurableTablesString+" or a custom table name"+
				". Unsetting means reverting the link target to null."+
				" Updates to the database are committed in batches, the default batch size is 250.") 
public class MultiUnsetLinkTargetCommand extends MultiLinkTargetUpdateCommand {

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}
	
	@Override
	public UpdateResult execute(CommandContext cmdContext) {
		return executeUpdates(cmdContext);
	}

	@Override
	protected void updateObject(CommandContext cmdContext, GlueDataObject object) {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		Project project = insideProjectMode.getProject();
		LinkUpdateContext linkUpdateContext = new LinkUpdateContext(project, getTableName(), getLinkName());
		PropertyCommandDelegate.executeLinkTargetUpdate(cmdContext, project, object, false, null, linkUpdateContext, LinkUpdateContext.UpdateType.UNSET);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new MultiLinkTargetUpdateCommand.TableNameInstantiator());
			registerVariableInstantiator("linkName", new MultiLinkTargetUpdateCommand.ToOneLinkInstantiator());

		}
		
	}

}
