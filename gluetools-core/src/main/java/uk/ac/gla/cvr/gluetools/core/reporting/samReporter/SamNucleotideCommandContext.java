package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.List;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.BaseSamReporterCommand.SamRefInfo;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class SamNucleotideCommandContext extends SamBaseNucleotideCommandContext {

	private TIntObjectMap<SamNucleotideResidueCount> relatedRefNtToInfo;

	public SamNucleotideCommandContext(SamReporter samReporter, SamRefInfo samRefInfo,
			List<QueryAlignedSegment> samRefToRelatedRefSegs, List<ReferenceSegment> selectedRefSegs, 
			SamRefSense samRefSense, SamRecordFilter samRecordFilter) {
		super(samReporter, samRefInfo, samRefToRelatedRefSegs, selectedRefSegs, samRefSense, samRecordFilter);
		this.relatedRefNtToInfo = new TIntObjectHashMap<SamNucleotideResidueCount>();
	}

	public TIntObjectMap<SamNucleotideResidueCount> getRelatedRefNtToInfo() {
		return relatedRefNtToInfo;
	}

	
	
}
