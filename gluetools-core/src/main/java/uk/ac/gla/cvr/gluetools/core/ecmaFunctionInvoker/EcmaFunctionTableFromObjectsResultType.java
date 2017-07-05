package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import java.util.ArrayList;
import java.util.List;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="tableFromObjectsResultType")
public class EcmaFunctionTableFromObjectsResultType extends EcmaFunctionResultType<EcmaFunctionTableResult> implements Plugin {

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
	public EcmaFunctionTableResult glueResultFromReturnObject(
			EcmaFunctionInvoker ecmaFunctionInvoker, String functionName,
			Object returnObj) {
		ScriptObjectMirror scriptObjectMirror = super.scriptObjectMirrorFromReturnObj(ecmaFunctionInvoker, functionName, returnObj);
		if(!scriptObjectMirror.isArray()) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, "JavaScript return object must be an array to make a GLUE result table");
		}
		List<ScriptObjectMirror> rowJsObjects = new ArrayList<ScriptObjectMirror>();
		for(int i = 0; i < scriptObjectMirror.size(); i++) {
			Object value = scriptObjectMirror.getSlot(i);
			if(!(value instanceof ScriptObjectMirror)) {
				throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
						ecmaFunctionInvoker.getModuleName(), functionName, "JavaScript array items must be objects to make a GLUE result table");
			}
			rowJsObjects.add((ScriptObjectMirror) value);
		}
		return new EcmaFunctionTableResult(ecmaFunctionInvoker, functionName, rowJsObjects);
	}

	
	
}
