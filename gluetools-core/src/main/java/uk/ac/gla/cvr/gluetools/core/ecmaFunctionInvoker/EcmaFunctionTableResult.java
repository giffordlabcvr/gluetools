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
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;
import uk.ac.gla.cvr.gluetools.utils.DateUtils;

public class EcmaFunctionTableResult extends BaseTableResult<Map<String, Object>> {

	public EcmaFunctionTableResult(EcmaFunctionInvoker ecmaFunctionInvoker, String functionName, List<ScriptObjectMirror> rowJsObjects) {
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
				if(value == null){
					row.put(key, null);
				} else if(value instanceof String) {
					String string = (String) value;
					if(DateUtils.isDateString(string)) {
						row.put(key, DateUtils.parse(string));
					} else {
						row.put(key, string);
					}
				} else if(value instanceof Boolean) {
					row.put(key, (Boolean) value);
				} else if(value instanceof Number) {
					Number num = (Number) value;
					// javascript does not have integers, only floats
					// here we force integer if the number is mathematically an integer.
					double doubleVal = Math.round(num.doubleValue());
					if(doubleVal == num.doubleValue()) {
						row.put(key, num.intValue());
					} else {
						row.put(key, num.doubleValue());
					}
				} else {
					throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
							ecmaFunctionInvoker.getModuleName(), functionName, 
							"Cell value of type "+value.getClass().getSimpleName()+" cannot be put in a GLUE result table");
				}
			});
			rows.add(row);
		}
		return rows;
	}

}
