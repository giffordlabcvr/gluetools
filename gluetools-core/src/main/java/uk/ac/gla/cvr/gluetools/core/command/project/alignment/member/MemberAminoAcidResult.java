package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class MemberAminoAcidResult extends TableResult {

	public static final String 
		CODON = "codon",
		AMINO_ACID = "aminoAcid";


	public MemberAminoAcidResult(List<Map<String, Object>> rowData) {
		super("memberAminoAcidsResult", 
				Arrays.asList(
						CODON, 
						AMINO_ACID), 
				rowData);
	}

}
