package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.resultType;

import jdk.nashorn.internal.runtime.Undefined;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvoker;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="okFromNullResultType")
public class EcmaFunctionOkFromNullResultType extends EcmaFunctionResultType<OkResult>  {

	@Override
	public OkResult glueResultFromReturnObject(
			EcmaFunctionInvoker ecmaFunctionInvoker, String functionName,
			Object returnObj) {
		if(returnObj != null && !(returnObj instanceof Undefined)) {
			GlueLogger.getGlueLogger().warning("Casting non-null object to OK result (function "+
					functionName+", module "+ecmaFunctionInvoker.getModuleName()+")");
			GlueLogger.getGlueLogger().warning("For other result types consider "+
					"<documentResultType> or <tableFromObjectsResultType> in the module config");
		}
		return new OkResult();
	}

}
