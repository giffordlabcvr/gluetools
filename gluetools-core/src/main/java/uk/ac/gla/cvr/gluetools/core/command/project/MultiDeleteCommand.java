package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"multi-delete"}, 
		docoptUsages={"<cTable> (-w <whereClause> | -a) [-b <batchSize>]"},
		metaTags={CmdMeta.updatesDatabase},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify deleted objects", 
				"-a, --allObjects                               Delete all objects",
				"-b <batchSize>, --batchSize <batchSize>        Delete batch size" },
		description="Delete multiple objects", 
		furtherHelp="Deletions from the database are committed in batches, the default batch size is 250.\n"+
		"Certain objects will not be deleted: e.g. sequences that are reference sequences.") 
public class MultiDeleteCommand extends ProjectModeCommand<DeleteResult> {

	public static final String BATCH_SIZE = "batchSize";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_OBJECTS = "allObjects";
	public static final String CONFIGURABLE_TABLE = "cTable";

	private Boolean allObjects;
	private ConfigurableTable cTable;
	private Optional<Expression> whereClause;
	private int batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		cTable = PluginUtils.configureEnumProperty(ConfigurableTable.class, configElem, CONFIGURABLE_TABLE, true);
		allObjects = PluginUtils.configureBooleanProperty(configElem, ALL_OBJECTS, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);
		if( !allObjects && !whereClause.isPresent() ) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allObjects must be specified");
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		Class<? extends GlueDataObject> dataObjectClass = cTable.getDataObjectClass();
		String objectWord = dataObjectClass.getSimpleName()+"s";
		
		SelectQuery selectQuery = null;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(dataObjectClass, whereClause.get());
		} else {
			selectQuery = new SelectQuery(dataObjectClass);
		}
		GlueLogger.getGlueLogger().fine("Finding "+objectWord+" to delete");
		List<? extends GlueDataObject> objectsToDelete = 
				GlueDataObject.query(cmdContext, dataObjectClass, selectQuery);
		
		
		if(cTable.equals(ConfigurableTable.sequence)) {
			// filter out reference sequences
			objectsToDelete = objectsToDelete.stream()
					.filter(obj -> ((Sequence) obj).getReferenceSequences().isEmpty())
					.collect(Collectors.toList());		
		}
		List<Map<String, String>> pkMaps = objectsToDelete.stream().map(seq -> seq.pkMap()).collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Found "+pkMaps.size()+" "+objectWord);
		
		int numDeleted = 0;
		for(Map<String, String> pkMap: pkMaps) {
			GlueDataObject.delete(cmdContext, dataObjectClass, pkMap, false);
			numDeleted++;
			if(numDeleted % batchSize == 0) {
				cmdContext.commit();
				cmdContext.newObjectContext();
				GlueLogger.getGlueLogger().finest("Deleted "+numDeleted+" "+objectWord);
			}
		}
		cmdContext.commit();
		cmdContext.newObjectContext();
		GlueLogger.getGlueLogger().finest("Deleted "+numDeleted+" "+objectWord);
		return new DeleteResult(dataObjectClass, numDeleted);
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerEnumLookup("cTable", ConfigurableTable.class);
		}
	}
	
}
