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
