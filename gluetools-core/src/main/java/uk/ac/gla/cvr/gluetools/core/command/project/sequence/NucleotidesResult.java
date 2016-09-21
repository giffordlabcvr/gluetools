package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class NucleotidesResult extends MapResult {

	public NucleotidesResult(Integer beginIndex, Integer endIndex, String nucleotides) {
		super("nucleotidesResult", mapBuilder()
				.put("beginIndex", beginIndex)
				.put("endIndex", endIndex)
				.put("nucleotides", nucleotides));
	}
	public String getNucleotides() {
		return getCommandDocument().getString("nucleotides");
	}
	
	
}
