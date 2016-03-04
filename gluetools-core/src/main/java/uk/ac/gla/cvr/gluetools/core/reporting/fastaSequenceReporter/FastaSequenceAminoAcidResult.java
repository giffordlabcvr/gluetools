package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class FastaSequenceAminoAcidResult extends BaseTableResult<LabeledQueryAminoAcid> {

	public static final String 
		CODON_LABEL = "codonLabel",
		QUERY_NT = "queryNt",
		AC_REF_NT = "acRefNt",
		AMINO_ACID = "aminoAcid";


	public FastaSequenceAminoAcidResult(List<LabeledQueryAminoAcid> labeledQueryAminoAcids) {
		super("fastaSequenceAminoAcidsResult", labeledQueryAminoAcids,
				column(CODON_LABEL, lqaa -> lqaa.getLabeledAminoAcid().getLabeledCodon().getLabel()),
				column(QUERY_NT, lqaa -> lqaa.getQueryNt()),
				column(AC_REF_NT, lqaa -> lqaa.getLabeledAminoAcid().getLabeledCodon().getNtStart()),
				column(AMINO_ACID, lqaa -> lqaa.getLabeledAminoAcid().getAminoAcid()));
	}

}
