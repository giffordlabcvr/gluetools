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

public class EcmaFunctionTableFromDocumentResult extends EcmaFunctionBaseTableResult<ScriptObjectMirror> {

	public EcmaFunctionTableFromDocumentResult(EcmaFunctionInvoker ecmaFunctionInvoker, String functionName, ScriptObjectMirror jsDocument) {
		super(rootNameFromJsDocument(jsDocument, ecmaFunctionInvoker, functionName), 
				rowsFromJsDocument(jsDocument, ecmaFunctionInvoker, functionName), 
				tableColumnsFromRowJsDocument(jsDocument, ecmaFunctionInvoker, functionName));
	}

	private static TableColumn<ScriptObjectMirror>[] tableColumnsFromRowJsDocument(ScriptObjectMirror jsDocument,
			EcmaFunctionInvoker ecmaFunctionInvoker, String functionName) {
		String[] columnNames = columnNames(jsDocument, ecmaFunctionInvoker, functionName);
		@SuppressWarnings("unchecked")
		TableColumn<ScriptObjectMirror>[] tableColumns = new TableColumn[columnNames.length];
		for(int i = 0; i < tableColumns.length; i++) {
			final int iFinal = i;
			TableColumn<ScriptObjectMirror> tableColumn = 
					new TableColumn<ScriptObjectMirror>(columnNames[iFinal], valueArray -> jsValueToGlueDocValue(ecmaFunctionInvoker, functionName, valueArray.getSlot(iFinal)));
			
			tableColumns[i] = tableColumn;
		}
		return tableColumns;
	}

	private static List<ScriptObjectMirror> rowsFromJsDocument(ScriptObjectMirror jsDocument,
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
		List<ScriptObjectMirror> rows = new ArrayList<ScriptObjectMirror>(rowsScriptObjectMirror.size());
		for(int i = 0; i < rowsScriptObjectMirror.size(); i++) {
			Object rowSlotValue = rowsScriptObjectMirror.getSlot(i);
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
			rows.add(valueScriptObjectMirror);
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
		for(int i = 0; i < numColumns; i++) {
			Object slotValue = columnScriptObjectMirror.getSlot(i);
			Object columnNameObj = jsValueToGlueDocValue(ecmaFunctionInvoker, functionName, slotValue);
			if(columnNameObj == null) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, 
						"Null column name");
			}
			columnNames[i] = columnNameObj.toString();
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
