package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.List;

public class ModifiedLabeledCodon extends LabeledCodon {

	public ModifiedLabeledCodon(String featureName, String codonLabel, String modifierModuleName, List<Integer> dependentRefNts, int transcriptionIndex) {
		super(featureName, codonLabel, dependentRefNts, transcriptionIndex);
	}
}
