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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"multi-delete"}, 
		docoptUsages={"<tableName> (-w <whereClause> | -a) [-b <batchSize>]"},
		metaTags = {},
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

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
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
		GlueLogger.getGlueLogger().fine("Finding "+objectWord+" to delete");
		List<? extends GlueDataObject> objectsToDelete = 
				GlueDataObject.query(cmdContext, dataObjectClass, selectQuery);
		
		
		if(tableName.equals(ConfigurableTable.sequence.name())) {
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
			registerVariableInstantiator("tableName", new MultiFieldUpdateCommand.TableNameInstantiator());
		}
	}
	
}
