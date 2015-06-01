package uk.ac.gla.cvr.gluetools.core.collation.sequence.genbank;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;

public class ParsedGenbank implements CollatedSequence {

	private String sourceId;

	@Override
	public String getSourceId() {
		return sourceId;
	}
	
	
	
}
