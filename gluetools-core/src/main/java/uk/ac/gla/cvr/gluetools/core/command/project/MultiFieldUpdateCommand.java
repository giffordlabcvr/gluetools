package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ModelBuilder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class MultiFieldUpdateCommand extends ProjectModeCommand<UpdateResult> {

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

	protected final UpdateResult executeUpdates(CommandContext cmdContext) {
		Class<? extends GlueDataObject> dataObjectClass = cTable.getDataObjectClass();
		String objectWord = dataObjectClass.getSimpleName()+"s";
		
		SelectQuery selectQuery = null;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(dataObjectClass, whereClause.get());
		} else {
			selectQuery = new SelectQuery(dataObjectClass);
		}
		GlueLogger.getGlueLogger().fine("Finding "+objectWord+" to update");
		List<? extends GlueDataObject> objectsToUpdate = 
				GlueDataObject.query(cmdContext, dataObjectClass, selectQuery);
		List<Map<String, String>> pkMaps = objectsToUpdate.stream().map(seq -> seq.pkMap()).collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Found "+pkMaps.size()+" "+objectWord);

		int numUpdated = 0;
		for(Map<String, String> pkMap: pkMaps) {
			GlueDataObject object = GlueDataObject.lookup(cmdContext, dataObjectClass, pkMap);
			updateObject(cmdContext, object);
			numUpdated++;
			if(numUpdated % batchSize == 0) {
				cmdContext.commit();
				cmdContext.newObjectContext();
				GlueLogger.getGlueLogger().finest("Updated "+numUpdated+" "+objectWord);
			}
		}
		cmdContext.commit();
		cmdContext.newObjectContext();
		GlueLogger.getGlueLogger().finest("Updated "+numUpdated+" "+objectWord);
		return new UpdateResult(dataObjectClass, numUpdated);
	}

	protected ConfigurableTable getCTable() {
		return cTable;
	}
	
	protected abstract void updateObject(CommandContext cmdContext, GlueDataObject object);
	
	public static class ModifiableFieldInstantiator extends AdvancedCmdCompleter.VariableInstantiator {
		@Override
		@SuppressWarnings("rawtypes")
		protected List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			
			String cTableName = (String) bindings.get("cTable");
			ConfigurableTable cTable = null;
			try {
				cTable = ConfigurableTable.valueOf(cTableName);
			} catch(IllegalArgumentException iae) {
				return null;
			}
			return 
					getProjectMode(cmdContext).getProject().getModifiableFieldNames(cTable)
					.stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
		}
	}

	
}