package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;

public class MapResult extends CommandResult {

	public MapResult(String rootObjectName, MapBuilder mapBuilder) {
		this(rootObjectName, mapBuilder.build());
	}

	public MapResult(String rootObjectName, Map<String, Object> map) {
		super(rootObjectName);
		CommandDocument documentBuilder = getCommandDocument();
		map.forEach((name, value) -> {
			documentBuilder.set(name, value);
		});
	}

	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		CommandDocument commandDocument = getCommandDocument();
		renderCtx.output(commandDocument.getRootName());
		for(String fieldName: commandDocument.getFieldNames()) {
			Object value = commandDocument.getSimpleValue(fieldName);
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
		CommandDocument commandDocument = getCommandDocument();
		for(String fieldName : commandDocument.getFieldNames()) {
			map.put(fieldName, commandDocument.getSimpleValue(fieldName));
		}
		return map;
	}
	
	public static <D> Map<String, Object> mapFromDataObject(
			List<String> headers, D dataObject, 
			BiFunction<D, String, Object> resolveHeaderFunction) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for(String header: headers) {
			map.put(header, resolveHeaderFunction.apply(dataObject, header));
		}
		return map;
	}
	
	
}
