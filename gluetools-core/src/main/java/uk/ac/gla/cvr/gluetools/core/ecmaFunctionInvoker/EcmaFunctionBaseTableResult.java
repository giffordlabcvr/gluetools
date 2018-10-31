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

import java.util.List;

import jdk.nashorn.internal.runtime.Undefined;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.EcmaFunctionInvokerException.Code;
import uk.ac.gla.cvr.gluetools.utils.DateUtils;

public abstract class EcmaFunctionBaseTableResult<D> extends BaseTableResult<D> {

	@SafeVarargs
	public EcmaFunctionBaseTableResult(String rootDocumentName, List<D> rowObjects,
			TableColumn<D>... tableColumns) {
		super(rootDocumentName, rowObjects, tableColumns);
	}

	protected static Object jsValueToGlueDocValue(EcmaFunctionInvoker ecmaFunctionInvoker, String functionName,
			Object value) {
		Object glueDocValue;
		if(value == null || value instanceof Undefined){
			glueDocValue = null;
		} else if(value instanceof String) {
			String string = (String) value;
			if(DateUtils.isDateString(string)) {
				glueDocValue = DateUtils.parse(string);
			} else {
				glueDocValue = string;
			}
		} else if(value instanceof Boolean) {
			glueDocValue = (Boolean) value;
		} else if(value instanceof Number) {
			Number num = (Number) value;
			// javascript does not have integers, only floats
			// here we force integer if the number is mathematically an integer.
			double doubleVal = Math.round(num.doubleValue());
			if(doubleVal == num.doubleValue()) {
				glueDocValue = num.intValue();
			} else {
				glueDocValue = num.doubleValue();
			}
		} else {
			throw new EcmaFunctionInvokerException(Code.FUNCTION_RESULT_EXCEPTION, 
					ecmaFunctionInvoker.getModuleName(), functionName, 
					"Cannot translate JavaScript value of type "+value.getClass().getSimpleName()+" to a simple GLUE document value");
		}
		return glueDocValue;
	}

}
