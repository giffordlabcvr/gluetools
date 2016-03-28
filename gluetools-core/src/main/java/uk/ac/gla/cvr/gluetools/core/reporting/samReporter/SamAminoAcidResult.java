package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class SamAminoAcidResult extends BaseTableResult<LabeledAminoAcidReadCount> {

	public static final String 
		CODON_LABEL = "codonLabel",
		SAM_REF_NT = "samRefNt",
		ANC_CONSTR_REF_NT = "ancConstrRefNt",
		AMINO_ACID = "aminoAcid",
		READS_WITH_AA = "readsWithAA",
		PERCENT_AA_READS = "pctAaReads";


	public SamAminoAcidResult(List<LabeledAminoAcidReadCount> rows) {
		super("samAminoAcidsResult", 
				rows,
				column(CODON_LABEL, laarc -> laarc.getLabeledAminoAcid().getLabeledCodon().getCodonLabel()),
				column(SAM_REF_NT, laarc -> laarc.getSamRefNt()), 
				column(ANC_CONSTR_REF_NT, laarc -> laarc.getLabeledAminoAcid().getLabeledCodon().getNtStart()),
				column(AMINO_ACID, laarc -> laarc.getLabeledAminoAcid().getAminoAcid()), 
				column(READS_WITH_AA, laarc -> laarc.getReadsWithAminoAcid()), 
				column(PERCENT_AA_READS, laarc -> laarc.getPercentReadsWithAminoAcid()));
	}

}
