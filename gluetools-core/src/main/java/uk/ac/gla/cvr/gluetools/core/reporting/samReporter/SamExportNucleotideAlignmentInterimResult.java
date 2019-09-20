package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;
public class SamExportNucleotideAlignmentInterimResult extends SamBaseNucleotideCommandInterimResult {
	private Map<String, DNASequence> fastaMap;

	public SamExportNucleotideAlignmentInterimResult(Map<String, DNASequence> fastaMap) {
		super();
		this.fastaMap = fastaMap;
	}

	public Map<String, DNASequence> getFastaMap() {
		return fastaMap;
	}
}
