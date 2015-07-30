package uk.ac.gla.cvr.gluetools.ws;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;

import com.mysql.jdbc.AbandonedConnectionCleanupThread;

@WebListener
@ApplicationPath("/")
public class GlueApplication extends ResourceConfig implements ServletContextListener {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.ws");
	
	public GlueApplication() {
		super();
		
		/** example... */
		final Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.path("helloworld");
 
        final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
        methodBuilder.produces(MediaType.TEXT_PLAIN_TYPE)
                .handledBy(new Inflector<ContainerRequestContext, String>() {
 
            @Override
            public String apply(ContainerRequestContext containerRequestContext) {
                return "Hello World!";
            }
        });
 
        final Resource resource = resourceBuilder.build();
        registerResources(resource);
		
		
    	registerInstances(new GlueRequestHandler());
	}


	@Override
	public void contextInitialized(ServletContextEvent sce) {
        String configFilePath = null;
        Boolean migrateSchema = null;
		try {
			Context ctx = new InitialContext();
	        configFilePath = (String) ctx.lookup("java:comp/env/configFilePath");
	        migrateSchema = (Boolean) ctx.lookup("java:comp/env/migrateSchema");
		} catch (NamingException e) {
			throw new GlueApplicationException("JNDI error. Please ensure the correct webapp config file exists in $CATALINA_BASE/conf/[enginename]/[hostname]/ or elsewhere", e);
		}
    	GluetoolsEngine.initInstance(configFilePath, migrateSchema);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		GluetoolsEngine.shutdown();
		cleanupMySQL();
	}
    
	private void cleanupMySQL() {
		Enumeration<Driver> drivers = DriverManager.getDrivers();
        Driver d = null;
        while(drivers.hasMoreElements()) {
            try {
                d = drivers.nextElement();
                DriverManager.deregisterDriver(d);
                logger.warning(String.format("Driver %s deregistered", d));
            } catch (SQLException ex) {
                logger.warning(String.format("Error deregistering driver %s: %s", d, ex.getMessage()));
            }
        }
        try {
            AbandonedConnectionCleanupThread.shutdown();
        } catch (InterruptedException e) {
            logger.warning("SEVERE problem cleaning up: " + e.getMessage());
            e.printStackTrace();
        }
     }
    
    
}