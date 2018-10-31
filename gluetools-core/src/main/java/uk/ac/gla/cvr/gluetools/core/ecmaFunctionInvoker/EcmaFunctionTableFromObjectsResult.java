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
package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class EcmaFunctionTableFromObjectsResult extends EcmaFunctionBaseTableResult<Map<String, Object>> {

	public EcmaFunctionTableFromObjectsResult(EcmaFunctionInvoker ecmaFunctionInvoker, String functionName, List<ScriptObjectMirror> rowJsObjects) {
		super("ecmaFunctionTableResult", rowsFromRowJsObjects(rowJsObjects, ecmaFunctionInvoker, functionName), tableColumnsFromRowJsObjects(rowJsObjects));
	}

	@SuppressWarnings("unchecked")
	private static TableColumn<Map<String, Object>>[] tableColumnsFromRowJsObjects(List<ScriptObjectMirror> rowJsObjects) {
		Map<String, TableColumn<Map<String, Object>>> columnsMap = new LinkedHashMap<String, TableColumn<Map<String, Object>>>();
		for(ScriptObjectMirror rowJsObject: rowJsObjects) {
			for(String key: rowJsObject.keySet()) {
				if(!columnsMap.containsKey(key)) {
					columnsMap.put(key, new TableColumn<Map<String,Object>>(key, m -> m.get(key)));
				}
			}
		}
		return columnsMap.values().toArray(new TableColumn[]{});
	}

	private static List<Map<String, Object>> rowsFromRowJsObjects(List<ScriptObjectMirror> rowJsObjects,
			EcmaFunctionInvoker ecmaFunctionInvoker, String functionName) {
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		for(ScriptObjectMirror rowJsObject: rowJsObjects) {
			Map<String, Object> row = new LinkedHashMap<String, Object>();
			rowJsObject.forEach((key, value) -> {
				row.put(key, jsValueToGlueDocValue(ecmaFunctionInvoker, functionName, value));
			});
			rows.add(row);
		}
		return rows;
	}

}
