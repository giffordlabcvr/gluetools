package uk.ac.gla.cvr.gluetools.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SortOrder;

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
				expression = Expression.fromString(expressionString);
			} catch(ExpressionException ee) {
				throw new CayenneUtilsException(CayenneUtilsException.Code.INVALID_CAYENNE_EXPRESSION, expressionString, ee.getLocalizedMessage());
			}
		}
		return expression;
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
