package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMRecord;

public interface SamPairedParallelProcessor<T> {

	public T createContext();
	
	public void processPair(T context, SAMRecord read1, SAMRecord read2);

	public void processSingleton(T context, SAMRecord read);

	public T reduceContexts(T context1, T context2);
	
}
