package uk.ac.gla.cvr.gluetools.core.logging;

import java.util.logging.Level;
import java.util.logging.Logger;


public class GlueLogger {

	public static final String[] ALL_LOG_LEVELS = new String[]{
			Level.OFF.getName(), Level.SEVERE.getName(), Level.WARNING.getName(), Level.INFO.getName(),
			Level.CONFIG.getName(), Level.FINE.getName(), Level.FINER.getName(), Level.FINEST.getName(),
			Level.ALL.getName()};
	
	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");
	
	static {
		logger.setLevel(Level.INFO);
	}
	
	public static Logger getGlueLogger() {
		return logger;
	}
	
	public static void setLogLevel(Level level) {
		logger.setLevel(level);
	}
	
	public static void log(Level level, String msg) {
		logger.log(level, msg);
	}

	public static void log(String msg) {
		logger.log(logger.getLevel(), msg);
	}

	
}
