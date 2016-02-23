package uk.ac.gla.cvr.gluetools.core.reporting.custom;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class FindRavsResult extends TableResult {

	public static final String 
		SAM_FILE_NAME = "samFileName",
		GENOTYPE = "genotype",
		FEATURE_NAME = "featureName",
		CODON = "codon",
		AMINO_ACID = "aminoAcid",
		NUM_READS = "numReads",
		PERCENT_READS = "pctReads",
		GENOTYPE_NUM = "genotypeNum",
		GENOTYPE_PCT = "genotypePct";
	
	
	public FindRavsResult(List<Map<String, Object>> rowData) {
		super("findRavsResult", 
				Arrays.asList(
						SAM_FILE_NAME, 
						GENOTYPE,
						FEATURE_NAME, 
						CODON, 
						AMINO_ACID, 
						NUM_READS, 
						PERCENT_READS, 
						GENOTYPE_NUM, 
						GENOTYPE_PCT), 
				rowData);
	}

}
