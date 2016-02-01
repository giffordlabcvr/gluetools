package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class SamAminoAcidsResult extends TableResult {

	public static final String 
		GLUE_REFERENCE_CODON = "glueReferenceCodon",
		SAM_REFERENCE_BASE = "samReferenceBase",
		AMINO_ACID = "aminoAcid",
		READS_WITH_AA = "readsWithAA";


	public SamAminoAcidsResult(List<Map<String, Object>> rowData) {
		super("samAminoAcidsResult", 
				Arrays.asList(
						GLUE_REFERENCE_CODON, 
						SAM_REFERENCE_BASE, 
						AMINO_ACID, 
						READS_WITH_AA), 
				rowData);
	}

}
