package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import htsjdk.samtools.SAMSequenceRecord;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class ListSamReferenceResult extends BaseTableResult<SAMSequenceRecord> {

	public ListSamReferenceResult(List<SAMSequenceRecord> samSequenceRecords) {
		super("listSamReferenceResult", samSequenceRecords, 
				column("name", samSeqRec -> samSeqRec.getSequenceName()),
				column("length", samSeqRec -> samSeqRec.getSequenceLength()),
				column("index", samSeqRec -> samSeqRec.getSequenceIndex()),
				column("species", samSeqRec -> samSeqRec.getSpecies()),
				column("assembly", samSeqRec -> samSeqRec.getAssembly())
		);
	}

}
