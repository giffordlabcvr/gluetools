package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class FastaSequenceAminoAcidResult extends TableResult {

	public static final String 
		CODON = "codon",
		NT_START = "ntStart",
		AMINO_ACID = "aminoAcid";


	public FastaSequenceAminoAcidResult(List<Map<String, Object>> rowData) {
		super("fastaSequenceAminoAcidsResult", 
				Arrays.asList(
						CODON,
						NT_START,
						AMINO_ACID), 
				rowData);
	}

}
