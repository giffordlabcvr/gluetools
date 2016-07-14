package uk.ac.gla.cvr.gluetools.programs.mafft.add;

import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;

public class MafftAddResult {

	private Map<String, DNASequence> alignmentWithQuery;

	public Map<String, DNASequence> getAlignmentWithQuery() {
		return alignmentWithQuery;
	}

	public void setAlignmentWithQuery(Map<String, DNASequence> alignmentWithQuery) {
		this.alignmentWithQuery = alignmentWithQuery;
	}
}
