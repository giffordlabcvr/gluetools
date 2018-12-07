package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public class SimpleReadLogger extends ReadLogger {
	private int pairs = 0;
	private int singletons = 0;
	private int totalReads = 0;
	
	public SimpleReadLogger() {
		
	}
	
	public synchronized void logPair() {
		totalReads++;
		if(totalReads % INTERVAL == 0) {
			printMessage();
		}
		totalReads++;
		pairs++;
		if(totalReads % INTERVAL == 0) {
			printMessage();
		}
	}

	public synchronized void logSingleton() {
		singletons++;
		totalReads++;
		if(totalReads % INTERVAL == 0) {
			printMessage();
		}
	}
	
	public void printMessage() {
		GlueLogger.getGlueLogger().finest("Processed "+totalReads+" reads, ("+pairs+" pairs, "+singletons+" singletons)");
	}
	
}