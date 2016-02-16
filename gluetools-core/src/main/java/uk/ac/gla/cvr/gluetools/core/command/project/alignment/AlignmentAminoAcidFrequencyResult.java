package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class AlignmentAminoAcidFrequencyResult extends TableResult {

	public static final String 
		CODON = "codon",
		AMINO_ACID = "aminoAcid",
		NUM_MEMBERS = "numMembers";


	public AlignmentAminoAcidFrequencyResult(List<Map<String, Object>> rowData) {
		super("alignmentAminoAcidFrequencyResult", 
				Arrays.asList(
						CODON, 
						AMINO_ACID, 
						NUM_MEMBERS), 
				rowData);
	}

}
