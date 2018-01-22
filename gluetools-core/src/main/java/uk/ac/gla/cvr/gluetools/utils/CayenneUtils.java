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
package uk.ac.gla.cvr.gluetools.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SortOrder;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public class CayenneUtils {

	public static Module createCayenneDbConfigModule(String cacheSizeFinal,
			String jdbcDriverClass, String jdbcUrl, Optional<String> username,
			Optional<String> password) {
		Module dbConfigModule = new Module() {
			  @Override
			  public void configure(Binder binder) {
				MapBuilder<Object> map = binder.bindMap(Constants.PROPERTIES_MAP)
			       .put(Constants.JDBC_DRIVER_PROPERTY, jdbcDriverClass)
			       .put(Constants.JDBC_URL_PROPERTY, jdbcUrl)
			       .put(Constants.QUERY_CACHE_SIZE_PROPERTY, cacheSizeFinal);
				username.ifPresent(u -> map.put(Constants.JDBC_USERNAME_PROPERTY, u));
				password.ifPresent(p -> map.put(Constants.JDBC_PASSWORD_PROPERTY, p));
			  }
		};
		return dbConfigModule;
	}

	public static Expression parseExpression(String expressionString) {
		Expression expression = null;
		if(expressionString != null) {
			try {
				expression = expressionFromString(expressionString);
			} catch(ExpressionException ee) {
				throw new CayenneUtilsException(CayenneUtilsException.Code.INVALID_CAYENNE_EXPRESSION, expressionString, ee.getLocalizedMessage());
			}
		}
		return expression;
	}

	public static Expression expressionFromString(String expressionString) {
		Map<String,Object> paramsMap = new LinkedHashMap<String,Object>();
		Pattern glueDatePattern = Pattern.compile("#gluedate\\([^\\)]*\\)");
		StringBuffer finalExprBuf = new StringBuffer();
		Matcher matcher = glueDatePattern.matcher(expressionString);
		int appendStart = 0;
		int dateIndex = 1;
		while(matcher.find()) {
			finalExprBuf.append(expressionString.substring(appendStart, matcher.start()));
			String key = "gluedate"+dateIndex;
			finalExprBuf.append("$"+key);
			Date date = DateUtils.parse(expressionString.substring(matcher.start()+"#gluedate(".length(), matcher.end()-")".length()));
			paramsMap.put(key, date);
			dateIndex++;
			appendStart = matcher.end();
		}
		finalExprBuf.append(expressionString.substring(appendStart, expressionString.length()));
		String finalExprString = finalExprBuf.toString();
		Expression expression = Expression.fromString(finalExprString);
		if(paramsMap.isEmpty()) {
			return expression;
		} else {
			GlueLogger.getGlueLogger().log(Level.FINEST, "Transformed expression to: "+finalExprString);
			GlueLogger.getGlueLogger().log(Level.FINEST, "Params map was: "+paramsMap);
			return expression.expWithParameters(paramsMap, false);
		}
	}

	public static List<Ordering> sortPropertiesToOrderings(String sortProperties) {
		String[] orderingTerms = sortProperties.split(",");
		List<Ordering> orderings = new ArrayList<Ordering>();
		for(String orderingTerm: orderingTerms) {
			String pathSpec = orderingTerm;
			SortOrder sortOrder = SortOrder.ASCENDING;
			if(orderingTerm.startsWith("+")) {
				pathSpec = orderingTerm.substring(1);
			} else if(orderingTerm.startsWith("-")) {
				pathSpec = orderingTerm.substring(1);
				sortOrder = SortOrder.DESCENDING;
			} 
			Ordering ordering = new Ordering(pathSpec, sortOrder);
			ordering.setNullSortedFirst(false);
			orderings.add(ordering);
		}
		return orderings;
	}
	
}
