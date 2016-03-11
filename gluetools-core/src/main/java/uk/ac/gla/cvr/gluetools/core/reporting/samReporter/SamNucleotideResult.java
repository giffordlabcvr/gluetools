package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class SamNucleotideResult extends BaseTableResult<NucleotideReadCount> {

	public static final String 
		SAM_REFERENCE_NT = "samRefNt",
		AC_REFERENCE_NT = "acRefNt",
		READS_WITH_A = "readsWithA",
		READS_WITH_C = "readsWithC",
		READS_WITH_G = "readsWithG",
		READS_WITH_T = "readsWithT";


	public SamNucleotideResult(List<NucleotideReadCount> rowData) {
		super("samNucleotidesResult", 
				rowData,
				column(SAM_REFERENCE_NT, nrc -> nrc.getSamRefNt()),
				column(AC_REFERENCE_NT, nrc -> nrc.getAcRefNt()), 
				column(READS_WITH_A, nrc -> nrc.getReadsWithA()),
				column(READS_WITH_C, nrc -> nrc.getReadsWithC()), 
				column(READS_WITH_G, nrc -> nrc.getReadsWithG()), 
				column(READS_WITH_T, nrc -> nrc.getReadsWithT()));
	}

}
