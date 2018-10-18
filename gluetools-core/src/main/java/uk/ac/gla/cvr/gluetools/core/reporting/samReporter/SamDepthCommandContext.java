package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.List;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.BaseSamReporterCommand.SamRefInfo;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamReporter.SamRefSense;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class SamDepthCommandContext extends SamBaseNucleotideCommandContext {

	private TIntObjectMap<SamContributingReadsCount> relatedRefNtToInfo;

	public SamDepthCommandContext(SamReporter samReporter, SamRefInfo samRefInfo,
			List<QueryAlignedSegment> samRefToRelatedRefSegs, SamRefSense samRefSense,
			SamRecordFilter samRecordFilter) {
		super(samReporter, samRefInfo, samRefToRelatedRefSegs, samRefSense, samRecordFilter);
		this.relatedRefNtToInfo = new TIntObjectHashMap<SamContributingReadsCount>();
	}

	public TIntObjectMap<SamContributingReadsCount> getRelatedRefNtToInfo() {
		return relatedRefNtToInfo;
	}

}
