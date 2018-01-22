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
