package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class SamAminoAcidResult extends TableResult {

	public static final String 
		CODON = "codon",
		SAM_REF_NT = "samReferenceNt",
		AMINO_ACID = "aminoAcid",
		READS_WITH_AA = "readsWithAA",
		PERCENT_AA_READS = "pctAaReads";


	public SamAminoAcidResult(List<Map<String, Object>> rowData) {
		super("samAminoAcidsResult", 
				Arrays.asList(
						CODON, 
						SAM_REF_NT, 
						AMINO_ACID, 
						READS_WITH_AA, 
						PERCENT_AA_READS), 
				rowData);
	}

}
