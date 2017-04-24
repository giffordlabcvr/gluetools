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

@CommandClass( 
		commandWords={"multi-render"}, 
		docoptUsages={"<tableName> (-w <whereClause> | -a) [-b <batchSize>] [<rendererModuleName>]"},
		metaTags={},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify rendered objects", 
				"-a, --allObjects                               Render all objects",
				"-b <batchSize>, --batchSize <batchSize>        Render batch size" },
		description="Render multiple objects", 
		furtherHelp="Renders are done in batches, the default batch size is 250.\n"+
				"The supplied <rendererModuleName> refers to a module implementing the IObjectRenderer interface.\n"+
				"If no <rendererModuleName> is supplied, a default renderer is used.") 
public class MultiRenderCommand extends ProjectModeCommand<MultiRenderResult> {

	public static final String BATCH_SIZE = "batchSize";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String ALL_OBJECTS = "allObjects";
	public static final String TABLE_NAME = "tableName";
	public static final String RENDERER_MODULE_NAME = "rendererModuleName";
	
	private Boolean allObjects;
	private String tableName;
	private Optional<Expression> whereClause;
	private int batchSize;
	private String rendererModuleName;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		tableName = PluginUtils.configureStringProperty(configElem, TABLE_NAME, true);
		allObjects = PluginUtils.configureBooleanProperty(configElem, ALL_OBJECTS, true);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);
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
		String objectWord = dataObjectClass.getSimpleName()+"s";
		GlueLogger.getGlueLogger().fine("Finding "+objectWord+" to render");

		List<? extends GlueDataObject> objectsToRender = 
				GlueDataObject.query(cmdContext, dataObjectClass, selectQuery);
		
		
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
			if(numRendered % batchSize == 0) {
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
