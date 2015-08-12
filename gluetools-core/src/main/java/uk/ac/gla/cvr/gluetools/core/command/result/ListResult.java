package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;

// TODO -- default list command for web interface should return only IDs
// TODO -- some kind of pagination for list commands.

public class ListResult extends TableResult {

	public static final String LIST_RESULT = "listResult";
	public static final String OBJECT_TYPE = "objectType";


	public <D extends GlueDataObject> ListResult(Class<D> objectClass, List<D> results) {
		this(objectClass, results, propertyPaths(objectClass));
	}	
	
	public <D extends GlueDataObject> ListResult(Class<D> objectClass, List<D> results, List<String> propertyPaths) {
		super(LIST_RESULT, propertyPaths, rowDataFromResults(results, propertyPaths));
		getDocumentBuilder().set(OBJECT_TYPE, objectClass.getSimpleName());
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		StringWriter stringWriter = new StringWriter();
		List<String> columnHeaders = super.getColumnHeaders();
		List<Map<String, Object>> listOfMaps = asListOfMaps();
		super.renderToStringWriter(stringWriter, columnHeaders, listOfMaps);
		String objectType = getDocumentReader().stringValue(OBJECT_TYPE);
		renderCtx.output(stringWriter.toString()+objectType+"s found: "+listOfMaps.size());
	}

	
	
	private static <D extends GlueDataObject> List<String> propertyPaths(Class<D> objectClass) {
		return Arrays.asList(objectClass.getAnnotation(GlueDataClass.class).defaultListColumns());
	}
	
	private static <D extends GlueDataObject> List<Map<String, Object>> rowDataFromResults(
			List<D> results, List<String> propertyPaths) {
		List<Map<String, Object>> listOfMaps = new ArrayList<Map<String, Object>>();
		for(D object: results) {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			for(String propertyPath: propertyPaths) {
				map.put(propertyPath, object.readNestedProperty(propertyPath));
			}
			listOfMaps.add(map);
		}
		return listOfMaps;
	}
	

}
