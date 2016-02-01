package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMRecord;

public interface SamRecordFilter {

	public boolean recordPasses(SAMRecord record);
	
}
