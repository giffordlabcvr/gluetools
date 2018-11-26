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
package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.resultType;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvoker;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionTableFromObjectsResult;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="tableFromObjectsResultType")
public class EcmaFunctionTableFromObjectsResultType extends EcmaFunctionResultType<EcmaFunctionTableFromObjectsResult> implements Plugin {

	// String used in table rendering
	public static final String OBJECT_TYPE = "objectType";

	private String objectType;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		objectType = PluginUtils.configureStringProperty(configElem, OBJECT_TYPE, false);
	}

	public String getObjectType() {
		return objectType;
	}

	@Override
	public EcmaFunctionTableFromObjectsResult glueResultFromReturnObject(
			EcmaFunctionInvoker ecmaFunctionInvoker, String functionName,
			Object returnObj) {
		ScriptObjectMirror scriptObjectMirror = super.scriptObjectMirrorFromReturnObj(ecmaFunctionInvoker, functionName, returnObj);
		if(!scriptObjectMirror.isArray()) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, "JavaScript return object must be an array to make a GLUE result table");
		}
		List<ScriptObjectMirror> rowJsObjects = new ArrayList<ScriptObjectMirror>();
		for(Object value: scriptObjectMirror.values()) {
			if(!(value instanceof ScriptObjectMirror)) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, "JavaScript array items must be objects to make a GLUE result table");
			}
			rowJsObjects.add((ScriptObjectMirror) value);
		}
		return new EcmaFunctionTableFromObjectsResult(ecmaFunctionInvoker, functionName, rowJsObjects);
	}

	
	
}
