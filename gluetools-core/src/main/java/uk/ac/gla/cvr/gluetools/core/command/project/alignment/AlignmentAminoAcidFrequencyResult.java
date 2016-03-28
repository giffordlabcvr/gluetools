package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcidFrequency;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class AlignmentAminoAcidFrequencyResult extends BaseTableResult<LabeledAminoAcidFrequency> {

	public static final String 
		CODON = "codon",
		AMINO_ACID = "aminoAcid",
		NUM_MEMBERS = "numMembers",
		PERCENTAGE_MEMBERS = "pctMembers";


	public AlignmentAminoAcidFrequencyResult(List<LabeledAminoAcidFrequency> rowData) {
		super("alignmentAminoAcidFrequencyResult", rowData,
				column(CODON, laaf -> laaf.getLabeledAminoAcid().getLabeledCodon().getCodonLabel()),
				column(AMINO_ACID, laaf -> laaf.getLabeledAminoAcid().getAminoAcid()),
				column(NUM_MEMBERS, laaf -> laaf.getNumMembers()),
				column(PERCENTAGE_MEMBERS, laaf -> laaf.getPctMembers()));
	}

}
