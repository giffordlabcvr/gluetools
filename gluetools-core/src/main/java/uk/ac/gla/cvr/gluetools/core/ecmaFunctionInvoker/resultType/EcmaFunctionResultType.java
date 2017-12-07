package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.resultType;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvoker;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class EcmaFunctionResultType<R extends CommandResult> implements Plugin {

	public abstract R glueResultFromReturnObject(EcmaFunctionInvoker ecmaFunctionInvoker, String functionName, Object returnObj);

	
	protected ScriptObjectMirror scriptObjectMirrorFromReturnObj(EcmaFunctionInvoker ecmaFunctionInvoker, String functionName, Object returnObj) {
		if(!(returnObj instanceof ScriptObjectMirror)) {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_INVOCATION_EXCEPTION, ecmaFunctionInvoker.getModuleName(), functionName, 
					"Result was not instance of ScriptObjectMirror (actual class "+returnObj.getClass().getSimpleName()+")");
		}
		return (ScriptObjectMirror) returnObj;
	}
}
