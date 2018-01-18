package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.AminoAcidStringFrequency;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcidFrequency;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class AlignmentAminoAcidStringsResult extends BaseTableResult<AminoAcidStringFrequency> {

	public static final String 
		AMINO_ACID_STRING = "aminoAcidString",
		NUM_MEMBERS = "numMembers",
		PERCENTAGE_MEMBERS = "pctMembers";


	public AlignmentAminoAcidStringsResult(List<AminoAcidStringFrequency> rowData) {
		super("alignmentAminoAcidStringsResult", rowData,
				column(AMINO_ACID_STRING, aasf -> aasf.getAminoAcidString()),
				column(NUM_MEMBERS, laaf -> laaf.getNumMembers()),
				column(PERCENTAGE_MEMBERS, laaf -> laaf.getPctMembers()));
	}

}
