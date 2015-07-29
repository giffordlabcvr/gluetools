package uk.ac.gla.cvr.gluetools.ws;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;

@ApplicationPath("glue")
public class GlueApplication extends ResourceConfig {
    public GlueApplication() {
        String configFilePath = null;
        Boolean migrateSchema = null;
		try {
			Context ctx = new InitialContext();
	        configFilePath = (String) ctx.lookup("java:comp/env/configFilePath");
	        migrateSchema = (Boolean) ctx.lookup("java:comp/env/migrateSchema");
		} catch (NamingException e) {
			throw new GlueApplicationException("JNDI error. Please ensure the correct webapp config file exists in $CATALINA_BASE/conf/[enginename]/[hostname]/", e);
		}
    	GluetoolsEngine gluetoolsEngine = GluetoolsEngine.initInstance(configFilePath, migrateSchema);
    	registerInstances(GluetoolsEngine.getInstance());
    }
}