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
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.render.defaultRenderer.DefaultObjectRenderer;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.IObjectRenderer;
import uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer.ObjectRenderer;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;

@CommandClass( 
		commandWords={"multi-render"}, 
		docoptUsages={"<tableName> (-w <whereClause> | -a) [-p <pageSize>] [-l <fetchLimit>] [-o <fetchOffset>] [-s <sortProperties>] [<rendererModuleName>]"},
		metaTags={},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify rendered objects", 
				"-a, --allObjects                               Render all objects",
				"-p <pageSize>, --pageSize <pageSize>                    Tune ORM page size",
				"-l <fetchLimit>, --fetchLimit <fetchLimit>              Limit max number of records",
				"-o <fetchOffset>, --fetchOffset <fetchOffset>           Record number offset",
				"-s <sortProperties>, --sortProperties <sortProperties>  Comma-separated sort properties" },
		description="Render multiple objects", 
		furtherHelp="Renders are done in batches, the default batch size is 250.\n"+
				"The supplied <rendererModuleName> refers to a module implementing the IObjectRenderer interface.\n"+
				"If no <rendererModuleName> is supplied, a default renderer is used.") 
public class MultiRenderCommand extends ProjectModeCommand<MultiRenderResult> {

	private static final int BATCH_SIZE = 250;

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_OBJECTS = "allObjects";
	public static final String TABLE_NAME = "tableName";
	public static final String RENDERER_MODULE_NAME = "rendererModuleName";
	public static final String PAGE_SIZE = "pageSize";
	public static final String FETCH_LIMIT = "fetchLimit";
	public static final String FETCH_OFFSET = "fetchOffset";
	public static final String SORT_PROPERTIES = "sortProperties";

	private Boolean allObjects;
	private String tableName;
	private Optional<Expression> whereClause;
	private int pageSize;
	private Optional<Integer> fetchLimit;
	private Optional<Integer> fetchOffset;
	private String sortProperties;
	private String rendererModuleName;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
		allObjects = PluginUtils.configureBooleanProperty(configElem, ALL_OBJECTS, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		sortProperties = Optional.ofNullable(PluginUtils.configureStringProperty(configElem, SORT_PROPERTIES, false)).orElse(null);
		pageSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, PAGE_SIZE, false)).orElse(250);
		fetchLimit = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_LIMIT, false));
		fetchOffset = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, FETCH_OFFSET, false));
		rendererModuleName = PluginUtils.configureStringProperty(configElem, RENDERER_MODULE_NAME, false);
		if( !allObjects && !whereClause.isPresent() ) {
			usageError();
		}
	}

	private void usageError() {
		throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Either whereClause or allObjects must be specified");
	}

	@Override
	public MultiRenderResult execute(CommandContext cmdContext) {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		Project project = insideProjectMode.getProject();
		project.checkTableName(tableName);
		Class<? extends GlueDataObject> dataObjectClass = project.getDataObjectClass(tableName);
		
		IObjectRenderer renderer = null;
		if(rendererModuleName == null) {
			renderer = DefaultObjectRenderer.getDefaultObjectRenderer(dataObjectClass);
		} else {
			renderer = ObjectRenderer.getRenderer(cmdContext, rendererModuleName);
		}
		
		SelectQuery selectQuery = null;
		if(whereClause.isPresent()) {
			selectQuery = new SelectQuery(dataObjectClass, whereClause.get());
		} else {
			selectQuery = new SelectQuery(dataObjectClass);
		}
		final SelectQuery finalSelectQuery = selectQuery;
		finalSelectQuery.setPageSize(pageSize);
		fetchLimit.ifPresent(limit -> finalSelectQuery.setFetchLimit(limit));
		fetchOffset.ifPresent(offset -> finalSelectQuery.setFetchOffset(offset));
			selectQuery.addOrderings(CayenneUtils.sortPropertiesToOrderings(project, tableName, sortProperties));

		String objectWord = dataObjectClass.getSimpleName()+"s";
		GlueLogger.getGlueLogger().fine("Finding "+objectWord+" to render");

		List<? extends GlueDataObject> objectsToRender = 
				GlueDataObject.query(cmdContext, dataObjectClass, finalSelectQuery);
		
		
		if(tableName.equals(ConfigurableTable.sequence.name())) {
			// filter out reference sequences
			objectsToRender = objectsToRender.stream()
					.filter(obj -> ((Sequence) obj).getReferenceSequences().isEmpty())
					.collect(Collectors.toList());		
		}
		List<Map<String, String>> pkMaps = objectsToRender.stream().map(seq -> seq.pkMap()).collect(Collectors.toList());
		GlueLogger.getGlueLogger().fine("Found "+pkMaps.size()+" "+objectWord);
		
		List<CommandDocument> renderResults = new ArrayList<CommandDocument>();
		
		int numRendered = 0;
		for(Map<String, String> pkMap: pkMaps) {
			GlueDataObject dataObject = GlueDataObject.lookup(cmdContext, dataObjectClass, pkMap, false);
			renderResults.add(renderer.render(cmdContext, dataObject).getCommandDocument());
			numRendered++;
			if(numRendered % BATCH_SIZE == 0) {
				cmdContext.newObjectContext();
				GlueLogger.getGlueLogger().finest("Rendered "+numRendered+" "+objectWord);
			}
		}
		cmdContext.newObjectContext();
		GlueLogger.getGlueLogger().finest("Rendered "+numRendered+" "+objectWord);
		return new MultiRenderResult(renderResults);
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("tableName", new MultiFieldUpdateCommand.TableNameInstantiator());
			registerDataObjectNameLookup("rendererModuleName", Module.class, Module.NAME_PROPERTY);
		}
	}
	
}
