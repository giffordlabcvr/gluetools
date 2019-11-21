package uk.ac.gla.cvr.gluetools.core.blastRotator;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class BlastSequenceRotatorResult extends BaseTableResult<RotationResultRow> {

	public BlastSequenceRotatorResult(List<RotationResultRow> rowObjects) {
		super("blastSequenceRotatorResult", rowObjects, 
				column("querySequenceId", row -> row.getQuerySequenceId()), 
				column("sequenceLength", row -> row.getSequenceLength()), 
				column("status", row -> row.getStatus().name()), 
				column("rotationNts", row -> row.getRotationNts()));
	}

}
