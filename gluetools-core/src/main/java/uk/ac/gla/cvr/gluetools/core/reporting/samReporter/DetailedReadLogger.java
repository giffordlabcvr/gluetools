package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public class DetailedReadLogger extends ReadLogger {
	private int balancedPairs = 0;
	private int unbalancedPairs = 0;
	private int singletonReads = 0;
	private int totalReads = 0;
	
	private int minusPlusPairs = 0;
	private int plusMinusPairs = 0;
	private int plusPlusPairs = 0;
	private int minusMinusPairs = 0;
	
	public DetailedReadLogger() {
		
	}
	
	public synchronized void logBalancedPair(boolean read1Fwd, boolean read2Fwd) {
		totalReads++;
		if(totalReads % INTERVAL == 0) {
			printMessage();
		}
		totalReads++;
		balancedPairs++;
		if(totalReads % INTERVAL == 0) {
			printMessage();
		}
		pairOrientations(read1Fwd, read2Fwd);
	}

	private void pairOrientations(boolean read1Fwd, boolean read2Fwd) {
		if(read1Fwd && read2Fwd) {
			plusPlusPairs++;
		} else if((!read1Fwd) && read2Fwd) {
			minusPlusPairs++;
		} else if(read1Fwd && !read2Fwd) {
			plusMinusPairs++;
		} else {
			minusMinusPairs++;
		}
	}

	public synchronized void logUnbalancedPair(boolean read1Fwd, boolean read2Fwd) {
		totalReads++;
		if(totalReads % INTERVAL == 0) {
			printMessage();
		}
		totalReads++;
		unbalancedPairs++;
		if(totalReads % INTERVAL == 0) {
			printMessage();
		}
		pairOrientations(read1Fwd, read2Fwd);
	}

	public synchronized void logSingleton() {
		singletonReads++;
		totalReads++;
		if(totalReads % INTERVAL == 0) {
			printMessage();
		}
	}
	
	public void printMessage() {
		GlueLogger.getGlueLogger().finest("Processed "+totalReads+" reads, ("+balancedPairs+" balanced pairs, "+unbalancedPairs+" unbalanced pairs, "+singletonReads+" singletons)");
		GlueLogger.getGlueLogger().finest("Pair orientations: +/- "+plusMinusPairs+", -/+ "+minusPlusPairs+", +/+ "+plusPlusPairs+", -/- "+minusMinusPairs);
	}
	
}