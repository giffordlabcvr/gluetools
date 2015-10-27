package uk.ac.gla.cvr.gluetools.core.logging;

import java.util.logging.Level;
import java.util.logging.Logger;


public class GlueLogger {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");
	
	public static Logger getGlueLogger() {
		return logger;
	}
	
	public static void setLogLevel(Level level) {
		logger.setLevel(level);
	}
	
	
	
}
