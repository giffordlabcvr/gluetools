package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.core.document.DocumentReader;

public class MapResult extends CommandResult {

	public MapResult(String rootObjectName, MapBuilder mapBuilder) {
		this(rootObjectName, mapBuilder.build());
	}

	public MapResult(String rootObjectName, Map<String, Object> map) {
		super(rootObjectName);
		DocumentBuilder documentBuilder = getDocumentBuilder();
		map.forEach((name, value) -> {
			documentBuilder.set(name, value);
		});
	}

	@Override
	protected void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		DocumentReader documentReader = getDocumentReader();
		renderCtx.output(documentReader.getName());
		for(String fieldName: documentReader.getFieldNames()) {
			Object value = documentReader.value(fieldName);
			if(value == null) {
				renderCtx.output("  "+fieldName+": -");
			} else {
				renderCtx.output("  "+fieldName+": "+value);
			}
		}
	}
	
	protected static class MapBuilder {
		private Map<String, Object> map = new LinkedHashMap<String, Object>();
		public MapBuilder put(String string, Object object) {
			map.put(string, object);
			return this;
		}
		public Map<String, Object> build() {
			return map;
		}
	}
	
	protected static MapBuilder mapBuilder() {
		return new MapBuilder();
	}
	
	public Map<String, Object> asMap() {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		DocumentReader documentReader = getDocumentReader();
		for(String fieldName : documentReader.getFieldNames()) {
			map.put(fieldName, documentReader.value(fieldName));
		}
		return map;
	}
	
	public static <D extends GlueDataObject> Map<String, Object> mapFromDataObject(
			List<String> headers, D dataObject, 
			BiFunction<D, String, Object> resolveHeaderFunction) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for(String header: headers) {
			map.put(header, resolveHeaderFunction.apply(dataObject, header));
		}
		return map;
	}
	
	public static class DefaultResolveHeaderFunction<D extends GlueDataObject> implements BiFunction<D, String, Object> {
		@Override
		public Object apply(D dataObject, String header) {
			return dataObject.readNestedProperty(header);
		}
		
	}
	
	
}
