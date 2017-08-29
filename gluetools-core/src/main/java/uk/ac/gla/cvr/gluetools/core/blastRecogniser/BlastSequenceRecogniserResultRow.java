package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.blastRecogniser.RecognitionCategoryResult.Direction;

public class BlastSequenceRecogniserResultRow {

	private String querySequenceId;
	private String categoryId;
	private Direction direction;
	
	public BlastSequenceRecogniserResultRow(String querySequenceId,
			String categoryId, Direction direction) {
		super();
		this.querySequenceId = querySequenceId;
		this.categoryId = categoryId;
		this.direction = direction;
	}

	public String getQuerySequenceId() {
		return querySequenceId;
	}

	public String getCategoryId() {
		return categoryId;
	}

	public Direction getDirection() {
		return direction;
	}

	public static List<BlastSequenceRecogniserResultRow> rowsFromMap(
			Map<String, List<RecognitionCategoryResult>> queryIdToCatResult) {
		List<BlastSequenceRecogniserResultRow> rows = new ArrayList<BlastSequenceRecogniserResultRow>();
		queryIdToCatResult.forEach((querySequenceId, catResults) -> {
			catResults.forEach(catResult -> 
				rows.add(new BlastSequenceRecogniserResultRow(querySequenceId, catResult.getCategoryId(), catResult.getDirection())));
		});
		return rows;
	}
	
	
	
}
