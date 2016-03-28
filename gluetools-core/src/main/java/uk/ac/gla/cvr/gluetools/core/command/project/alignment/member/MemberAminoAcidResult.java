package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class MemberAminoAcidResult extends BaseTableResult<LabeledQueryAminoAcid> {

	public static final String 
		CODON_LABEL = "codonLabel",
		MEMBER_NT = "memberNt",
		AC_REF_NT = "acRefNt",
		AMINO_ACID = "aminoAcid";


	public MemberAminoAcidResult(List<LabeledQueryAminoAcid> rowData) {
		super("memberAminoAcidsResult", 
				rowData, 
				column(CODON_LABEL, lqaa -> lqaa.getLabeledAminoAcid().getLabeledCodon().getCodonLabel()),
				column(MEMBER_NT, lqaa -> lqaa.getQueryNt()),
				column(AC_REF_NT, lqaa -> lqaa.getLabeledAminoAcid().getLabeledCodon().getNtStart()),
				column(AMINO_ACID, lqaa -> lqaa.getLabeledAminoAcid().getAminoAcid()));
	}

}
