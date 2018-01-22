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
package uk.ac.gla.cvr.gluetools.ws;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import uk.ac.gla.cvr.gluetools.core.GluetoolsEngine;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

import com.mysql.jdbc.AbandonedConnectionCleanupThread;

@WebListener
@ApplicationPath("/")
public class GlueApplication extends ResourceConfig implements ServletContextListener {

	public static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.ws");
	
	public GlueApplication() {
		super();
    	registerInstances(new GlueRequestHandler());
    	registerInstances(new GlueExceptionHandler());
    	register(MultiPartFeature.class);
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
		Level glueLogLevel = Level.FINEST;
		GlueLogger.setLogLevel(glueLogLevel);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(glueLogLevel);
		GlueLogger.getGlueLogger().addHandler(handler);
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