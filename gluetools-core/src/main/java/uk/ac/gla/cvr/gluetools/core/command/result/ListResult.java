package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

// TODO -- default list command for web interface should return only IDs
// TODO -- some kind of pagination for list commands.

public class ListResult extends TableResult {

	public static final String LIST_RESULT = "listResult";
	public static final String OBJECT_TYPE = "objectType";


	public <D extends GlueDataObject> ListResult(Class<D> objectClass, List<D> results) {
		this(objectClass, results, propertyPaths(objectClass));
	}	
	
	public <D extends GlueDataObject> ListResult(Class<D> objectClass, List<D> results, List<String> headers) {
		this(objectClass, results, headers, new MapResult.DefaultResolveHeaderFunction<D>());
	}

	
	public <D> ListResult(Class<D> objectClass, List<D> results, List<String> headers, 
			BiFunction<D, String, Object> resolveHeaderFunction) {
		super(LIST_RESULT, headers, listOfMapsFromDataObjects(results, headers, resolveHeaderFunction));
		getDocumentBuilder().set(OBJECT_TYPE, objectClass.getSimpleName());
	}

	
	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		List<String> columnHeaders = super.getColumnHeaders();
		List<Map<String, Object>> listOfMaps = asListOfMaps();
		String objectType = getDocumentReader().stringValue(OBJECT_TYPE);
		ArrayList<TablePage> tablePages = renderToTablePages(columnHeaders, listOfMaps, renderCtx);
		if(tablePages.size() == 0) {
			renderCtx.output(objectType+"s found: "+listOfMaps.size());
		} else if(tablePages.size() == 1) {
			renderCtx.output(tablePages.get(0).content+objectType+"s found: "+listOfMaps.size());
		} else {
			super.interactiveTableRender(renderCtx, objectType, tablePages);
		}
	}

	
	
	public static <D extends GlueDataObject> List<String> propertyPaths(Class<D> objectClass) {
		return Arrays.asList(Project.getDataClassAnnotation(objectClass).defaultListedProperties());
	}
	
	

}
