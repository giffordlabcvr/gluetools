package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.document.DocumentBuilder;
import uk.ac.gla.cvr.gluetools.core.document.DocumentReader;

public class MapResult extends CommandResult {

	public MapResult(String rootObjectName, MapBuilder mapBuilder) {
		super(rootObjectName);
		DocumentBuilder documentBuilder = getDocumentBuilder();
		mapBuilder.build().forEach((name, value) -> {
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
				renderCtx.output("  "+fieldName+": "+value);
			} else {
				renderCtx.output("  "+fieldName+": -");
			}
		}
	}
	
	protected static class MapBuilder {
		private Map<String, Object> map;
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
	
	
}
