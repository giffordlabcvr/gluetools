package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.BaseSamReporterCommand.SamRefInfo;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class SamBaseNucleotideCommandContext {
	private SamReporter samReporter;
	private SamRefInfo samRefInfo;
	private List<QueryAlignedSegment> samRefToRelatedRefSegs;
	private List<ReferenceSegment> selectedRefSegs;
	private SamRefSense samRefSense;
	private SamRecordFilter samRecordFilter;
	
	public SamBaseNucleotideCommandContext(SamReporter samReporter, SamRefInfo samRefInfo,
			List<QueryAlignedSegment> samRefToRelatedRefSegs, List<ReferenceSegment> selectedRefSegs, SamRefSense samRefSense,
			SamRecordFilter samRecordFilter) {
		super();
		this.samReporter = samReporter;
		this.samRefInfo = samRefInfo;
		this.samRefToRelatedRefSegs = samRefToRelatedRefSegs;
		this.selectedRefSegs = selectedRefSegs;
		this.samRefSense = samRefSense;
		this.samRecordFilter = samRecordFilter;
	}

	public SamReporter getSamReporter() {
		return samReporter;
	}

	public SamRefInfo getSamRefInfo() {
		return samRefInfo;
	}

	public List<QueryAlignedSegment> getSamRefToRelatedRefSegs() {
		return samRefToRelatedRefSegs;
	}

	public List<ReferenceSegment> getSelectedRefSegs() {
		return selectedRefSegs;
	}

	public SamRefSense getSamRefSense() {
		return samRefSense;
	}

	public SamRecordFilter getSamRecordFilter() {
		return samRecordFilter;
	}
	
	
	
}