package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class GenotypingCommandResult extends BaseTableResult<GenotypingResult> {

	public GenotypingCommandResult(List<GenotypingResult> genotypingResults) {
		super("genotypingCommandResult", genotypingResults,
				column("sequence", gtr -> gtr.getSequenceName()),
				column("groupingAlignmentName", gtr -> gtr.getGroupingAlignmentName()),
				column("closestMemberAlmtName", gtr -> gtr.getClosestMemberAlmtName()),
				column("closestMemberSourceName", gtr -> gtr.getClosestMemberAlmtName()),
				column("closestMemberSequenceID", gtr -> gtr.getClosestMemberSequenceID()),
				column("queryClosestMemberCoverage", gtr -> gtr.getQueryClosestMemberCoverage()),
				column("queryClosestMemberNtIdentity", gtr -> gtr.getQueryClosestMemberNtIdentity()));
	}
	
}
