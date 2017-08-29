package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class BlastSequenceRecogniserResult extends BaseTableResult<BlastSequenceRecogniserResultRow> {

	public BlastSequenceRecogniserResult(List<BlastSequenceRecogniserResultRow> rowObjects) {
		super("blastSequenceRecogniserResult", rowObjects, 
				column("querySequenceId", row -> row.getQuerySequenceId()), 
				column("categoryId", row -> row.getCategoryId()), 
				column("direction", row -> row.getDirection().name()));
	}

}
