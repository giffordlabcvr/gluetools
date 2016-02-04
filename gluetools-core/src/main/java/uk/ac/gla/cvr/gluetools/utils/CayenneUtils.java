package uk.ac.gla.cvr.gluetools.utils;

import java.util.Optional;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;

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

}
