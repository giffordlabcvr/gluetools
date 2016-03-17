package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class FeatureLocAminoAcidResult extends BaseTableResult<LabeledAminoAcid> {

	public static final String 
		CODON_LABEL = "codonLabel",
		REF_NT = "refNt",
		AMINO_ACID = "aminoAcid";


	public FeatureLocAminoAcidResult(List<LabeledAminoAcid> rowData) {
		super("featureLocAminoAcidResult", 
				rowData, 
				column(CODON_LABEL, laa -> laa.getLabeledCodon().getLabel()),
				column(REF_NT, laa -> laa.getLabeledCodon().getNtStart()),
				column(AMINO_ACID, laa -> laa.getAminoAcid()));
	}

}
