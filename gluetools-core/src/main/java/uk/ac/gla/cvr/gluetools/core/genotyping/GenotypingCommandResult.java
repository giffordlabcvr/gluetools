package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class GenotypingCommandResult extends BaseTableResult<GenotypingResult> {

	public GenotypingCommandResult(List<GenotypingResult> genotypingResults) {
		super("genotypingCommandResult", genotypingResults,
				column("sequence", gtr -> gtr.getSequenceName()),
				column("alignmentName", gtr -> gtr.getAlignmentName()),
				column("closestReference", gtr -> gtr.getClosestReference()),
				column("queryReferenceCoverage", gtr -> gtr.getQueryReferenceCoverage()),
				column("referenceNtIdentity", gtr -> gtr.getReferenceNtIdentity()));
	}
	
}
