package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class SamDepthResult extends BaseTableResult<NucleotideReadCount> {

	public static final String 
		SAM_REFERENCE_NT = "samRefNt",
		AC_REFERENCE_NT = "acRefNt",
		DEPTH = "depth";


	public SamDepthResult(List<NucleotideReadCount> rowData) {
		super("samDepthResult", 
				rowData,
				column(SAM_REFERENCE_NT, nrc -> nrc.getSamRefNt()),
				column(AC_REFERENCE_NT, nrc -> nrc.getAcRefNt()), 
				column(DEPTH, nrc -> nrc.getTotalContributingReads()));
	}

}
