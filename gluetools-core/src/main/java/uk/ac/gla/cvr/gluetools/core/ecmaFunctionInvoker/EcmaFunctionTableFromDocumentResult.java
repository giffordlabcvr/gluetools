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
import java.util.List;
import java.util.Set;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;

public class EcmaFunctionTableFromDocumentResult extends EcmaFunctionBaseTableResult<List<Object>> {

	public EcmaFunctionTableFromDocumentResult(EcmaFunctionInvoker ecmaFunctionInvoker, String functionName, ScriptObjectMirror jsDocument) {
		super(rootNameFromJsDocument(jsDocument, ecmaFunctionInvoker, functionName), 
				rowsFromJsDocument(jsDocument, ecmaFunctionInvoker, functionName), 
				tableColumnsFromRowJsDocument(jsDocument, ecmaFunctionInvoker, functionName));
	}

	private static TableColumn<List<Object>>[] tableColumnsFromRowJsDocument(ScriptObjectMirror jsDocument,
			EcmaFunctionInvoker ecmaFunctionInvoker, String functionName) {
		String[] columnNames = columnNames(jsDocument, ecmaFunctionInvoker, functionName);
		@SuppressWarnings("unchecked")
		TableColumn<List<Object>>[] tableColumns = new TableColumn[columnNames.length];
		for(int i = 0; i < tableColumns.length; i++) {
			final int iFinal = i;
			TableColumn<List<Object>> tableColumn = 
					new TableColumn<List<Object>>(columnNames[iFinal], valueList -> jsValueToGlueDocValue(ecmaFunctionInvoker, functionName, valueList.get(iFinal)));
			
			tableColumns[i] = tableColumn;
		}
		return tableColumns;
	}

	private static List<List<Object>> rowsFromJsDocument(ScriptObjectMirror jsDocument,
			EcmaFunctionInvoker ecmaFunctionInvoker, String functionName) {
		ScriptObjectMirror rootObject = getRootObject(jsDocument, ecmaFunctionInvoker, functionName);
		String[] columnNames = columnNames(jsDocument, ecmaFunctionInvoker, functionName);
		Object rowObj = rootObject.get("row");
		if(rowObj == null) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Root object for tableFromDocumentResultType has no 'row' field");
		}
		if(!(rowObj instanceof ScriptObjectMirror)) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Root object 'row' field for tableFromDocumentResultType must be an array");
		}
		ScriptObjectMirror rowsScriptObjectMirror = (ScriptObjectMirror) rowObj;
		if(!rowsScriptObjectMirror.isArray()) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Root object 'row' field for tableFromDocumentResultType must be an array");
		}
		List<List<Object>> rows = new ArrayList<List<Object>>(rowsScriptObjectMirror.size());
		for(Object rowSlotValue : rowsScriptObjectMirror.values()) {
			if(!(rowSlotValue instanceof ScriptObjectMirror)) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, 
						"Value in 'row' array for tableFromDocumentResultType must be an object");
				
			}
			ScriptObjectMirror rowScriptObjectMirror = (ScriptObjectMirror) rowSlotValue;
			if(rowScriptObjectMirror.isArray()) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, 
						"Value in 'row' array for tableFromDocumentResultType must be an object");
				
			}
			String[] keys = rowScriptObjectMirror.keySet().toArray(new String[]{});
			if(keys.equals(new String[] {"value"} )) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, 
						"Object in 'row' array for tableFromDocumentResultType must have a single 'value' field");
			}
			Object valueArrayObj = rowScriptObjectMirror.get("value");
			if(!(valueArrayObj instanceof ScriptObjectMirror)) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, 
						"Field 'value' in row for tableFromDocumentResultType must contain an array");
				
			}
			ScriptObjectMirror valueScriptObjectMirror = (ScriptObjectMirror) valueArrayObj;
			if(!valueScriptObjectMirror.isArray()) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, 
						"Field 'value' in row for tableFromDocumentResultType must contain an array");
			}
			if(valueScriptObjectMirror.size() != columnNames.length) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, 
						"Field 'value' in row for tableFromDocumentResultType must be an array of correct length");
			}
			rows.add(new ArrayList<Object>(valueScriptObjectMirror.values()));
		}
		return rows;
	}

	private static String[] columnNames(ScriptObjectMirror jsDocument, EcmaFunctionInvoker ecmaFunctionInvoker,
			String functionName) {
		ScriptObjectMirror rootObject = getRootObject(jsDocument, ecmaFunctionInvoker, functionName);
		Object columnObj = rootObject.get("column");
		if(columnObj == null) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Root object for tableFromDocumentResultType has no 'column' field");
		}
		if(!(columnObj instanceof ScriptObjectMirror)) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Root object 'column' field for tableFromDocumentResultType must be an array");
		}
		ScriptObjectMirror columnScriptObjectMirror = (ScriptObjectMirror) columnObj;
		if(!columnScriptObjectMirror.isArray()) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Root object 'column' field for tableFromDocumentResultType must be an array");
		}
		int numColumns = columnScriptObjectMirror.size();
		String[] columnNames = new String[numColumns];
		int i = 0;
		for(Object slotValue: columnScriptObjectMirror.values()) {
			Object columnNameObj = jsValueToGlueDocValue(ecmaFunctionInvoker, functionName, slotValue);
			if(columnNameObj == null) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, 
						"Null column name");
			}
			columnNames[i] = columnNameObj.toString();
			i++;
		}
		return columnNames;
	}

	private static ScriptObjectMirror getRootObject(ScriptObjectMirror jsDocument, EcmaFunctionInvoker ecmaFunctionInvoker,
			String functionName) {
		String rootName = rootNameFromJsDocument(jsDocument, ecmaFunctionInvoker, functionName);
		Object rootObj = jsDocument.get(rootName);
		if(!(rootObj instanceof ScriptObjectMirror)) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Root object for tableFromDocumentResultType must be an object");
		}
		ScriptObjectMirror rootScriptObjectMirror = (ScriptObjectMirror) rootObj;
		if(rootScriptObjectMirror.isArray()) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Root object for tableFromDocumentResultType must not be an array");
		}
		return rootScriptObjectMirror;
	} 
	
	private static String rootNameFromJsDocument(ScriptObjectMirror jsDocument, EcmaFunctionInvoker ecmaFunctionInvoker,
			String functionName) {
		Set<String> keySet = jsDocument.keySet();
		if(keySet.size() != 1) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"JavaScript object tableFromDocumentResultType must have a single field");

		}
		return keySet.iterator().next();
	}

}
