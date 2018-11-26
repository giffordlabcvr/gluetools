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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;

public class EcmaFunctionMapFromDocumentResult extends MapResult {

	public EcmaFunctionMapFromDocumentResult(EcmaFunctionInvoker ecmaFunctionInvoker,
			String functionName, ScriptObjectMirror jsDocument) {
		super(rootNameFromJsDocument(jsDocument, ecmaFunctionInvoker, functionName), 
				mapFromDocument(jsDocument, ecmaFunctionInvoker, functionName));
	}

	private static String rootNameFromJsDocument(ScriptObjectMirror jsDocument, EcmaFunctionInvoker ecmaFunctionInvoker,
			String functionName) {
		Set<String> keySet = jsDocument.keySet();
		if(keySet.size() != 1) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"JavaScript object mapFromDocumentResultType must have a single field");

		}
		return keySet.iterator().next();
	}
	
	private static Map<String, Object> mapFromDocument(ScriptObjectMirror jsDocument, EcmaFunctionInvoker ecmaFunctionInvoker,
			String functionName) {
		Object rootObj = jsDocument.values().iterator().next();
		if(!(rootObj instanceof ScriptObjectMirror)) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"JavaScript object mapFromDocumentResultType must have an object as its value");
		}
		ScriptObjectMirror rootScriptObj = (ScriptObjectMirror) rootObj;
		if(rootScriptObj.isArray()) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"JavaScript object mapFromDocumentResultType may not have an array as its value");
		}
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		rootScriptObj.forEach((k, v) -> {
			map.put(k, EcmaFunctionBaseTableResult.jsValueToGlueDocValue(ecmaFunctionInvoker, functionName, v));
		});
		return map;
	}

}
