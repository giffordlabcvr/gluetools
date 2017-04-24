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
import uk.ac.gla.cvr.gluetools.core.datamodel.link.Link;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.LinkException;
import uk.ac.gla.cvr.gluetools.core.datamodel.link.LinkException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class MultiLinkTargetUpdateCommand extends ProjectModeCommand<UpdateResult> {

	public static final String BATCH_SIZE = "batchSize";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_OBJECTS = "allObjects";
	public static final String TABLE_NAME = "tableName";
	public static final String LINK_NAME = "linkName";

	private Boolean allObjects;
	private String tableName;
	private String linkName;
	private Optional<Expression> whereClause;
	private int batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
		linkName = PluginUtils.configureStringProperty(configElem, LINK_NAME, true);
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
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		Project project = insideProjectMode.getProject();
		project.checkTableName(tableName);
		
		Class<? extends GlueDataObject> dataObjectClass = project.getDataObjectClass(tableName);
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

	protected String getTableName() {
		return tableName;
	}

	protected String getLinkName() {
		return linkName;
	}
	
	protected abstract void updateObject(CommandContext cmdContext, GlueDataObject object);
	
	public static class TableNameInstantiator extends AdvancedCmdCompleter.VariableInstantiator {
		@Override
		@SuppressWarnings("rawtypes")
		protected List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			return insideProjectMode.getProject().getTableNames()
					.stream().map(t -> new CompletionSuggestion(t, true)).collect(Collectors.toList());
		}
	}
	
	public static class ToOneLinkInstantiator extends AdvancedCmdCompleter.VariableInstantiator {
		@Override
		protected List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext,
				@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
				String prefix) {

			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			Project project = insideProjectMode.getProject();
			String tableName = (String) bindings.get("tableName");
			List<Link> linksForWhichSource = project.getLinksForWhichSource(tableName);
			List<CompletionSuggestion> suggestions = linksForWhichSource
					.stream()
					.filter(l -> !l.isToMany())
					.map(n -> new CompletionSuggestion(n.getSrcLinkName(), true))
					.collect(Collectors.toList());
			List<Link> linksForWhichDestination = project.getLinksForWhichDestination(tableName);
			suggestions.addAll(linksForWhichDestination.stream()
					.map(n -> new CompletionSuggestion(n.getDestLinkName(), true))
					.collect(Collectors.toList()));
			return suggestions;
		}
	}

	
}
