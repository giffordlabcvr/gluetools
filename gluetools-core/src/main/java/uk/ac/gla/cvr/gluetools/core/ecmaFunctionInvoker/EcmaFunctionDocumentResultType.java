package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.command.scripting.ScriptObjectMirrorUtils;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="documentResultType")
public class EcmaFunctionDocumentResultType extends EcmaFunctionResultType<EcmaFunctionDocumentResult>  {

	@Override
	public EcmaFunctionDocumentResult glueResultFromReturnObject(
			EcmaFunctionInvoker ecmaFunctionInvoker, String functionName,
			Object returnObj) {
		ScriptObjectMirror scriptObjMirror = super.scriptObjectMirrorFromReturnObj(ecmaFunctionInvoker, functionName, returnObj);
		CommandDocument resultCmdDoc = ScriptObjectMirrorUtils.scriptObjectMirrorToCommandDocument(scriptObjMirror);
		return new EcmaFunctionDocumentResult(resultCmdDoc);
	}

}
