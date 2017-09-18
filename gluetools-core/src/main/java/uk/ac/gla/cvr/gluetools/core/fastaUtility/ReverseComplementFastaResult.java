package uk.ac.gla.cvr.gluetools.core.fastaUtility;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class ReverseComplementFastaResult extends MapResult {

	public ReverseComplementFastaResult(String reverseComplementString) {
		super("reverseComplementFastaResult", mapBuilder().put("reverseComplement", reverseComplementString));
	}

}
