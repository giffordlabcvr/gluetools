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
