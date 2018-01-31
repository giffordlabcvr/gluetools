/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class MultiFieldUpdateCommand extends ProjectModeCommand<UpdateResult> {

	public static final String BATCH_SIZE = "batchSize";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_OBJECTS = "allObjects";
	public static final String TABLE_NAME = "tableName";

	private Boolean allObjects;
	private String tableName;
	private Optional<Expression> whereClause;
	private int batchSize;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
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
	
	protected abstract void updateObject(CommandContext cmdContext, GlueDataObject object);
	
	public static class TableNameInstantiator extends AdvancedCmdCompleter.VariableInstantiator {
		@Override
		@SuppressWarnings("rawtypes")
		public List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
			return insideProjectMode.getProject().getTableNames()
					.stream().map(t -> new CompletionSuggestion(t, true)).collect(Collectors.toList());
		}
	}
	
	public static class ModifiableFieldInstantiator extends AdvancedCmdCompleter.VariableInstantiator {
		@Override
		@SuppressWarnings("rawtypes")
		public List<CompletionSuggestion> instantiate(
				ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
				Map<String, Object> bindings, String prefix) {
			
			String tableName = (String) bindings.get("tableName");
			Project project = getProjectMode(cmdContext).getProject();
			if(project.getDataObjectClass(tableName) != null) {
				return project.getModifiableFieldNames(tableName)
						.stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
			}
			return new ArrayList<CompletionSuggestion>();
		}
	}

	
}
