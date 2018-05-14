package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public interface SamPairedParallelProcessor<T, R> {

	public void initContextForReader(T context, SamReader reader);
	
	public void processPair(T context, SAMRecord read1, SAMRecord read2);

	public void processSingleton(T context, SAMRecord read);

	public R contextResult(T context);
	
	public R reduceResults(R result1, R result2);
	
}
