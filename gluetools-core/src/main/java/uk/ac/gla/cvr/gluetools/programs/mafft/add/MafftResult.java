package uk.ac.gla.cvr.gluetools.programs.mafft.add;

import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;

public class MafftResult {

	private Map<String, DNASequence> resultAlignment;

	public Map<String, DNASequence> getResultAlignment() {
		return resultAlignment;
	}

	public void setResultAlignment(Map<String, DNASequence> resultAlignment) {
		this.resultAlignment = resultAlignment;
	}
}
