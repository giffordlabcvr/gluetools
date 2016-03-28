package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class FeatureLocListLabeledCodonsResult extends BaseTableResult<LabeledCodon> {

	public FeatureLocListLabeledCodonsResult(List<LabeledCodon> rowObjects) {
		super("showLabeledCodonsResult", rowObjects,
				column("referenceNt", lc -> lc.getNtStart()),
				column("codonLabel", lc -> lc.getCodonLabel()));
	}

}
