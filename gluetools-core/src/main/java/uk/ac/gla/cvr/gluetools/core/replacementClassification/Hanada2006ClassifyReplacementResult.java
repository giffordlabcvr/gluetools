package uk.ac.gla.cvr.gluetools.core.replacementClassification;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.translation.Hanada2006Classification;

public class Hanada2006ClassifyReplacementResult extends BaseTableResult<Hanada2006Classification.ReplacementClassification> {

	public Hanada2006ClassifyReplacementResult(List<Hanada2006Classification.ReplacementClassification> rowObjects) {
		super("hanada2006ClassifyReplacementResult", rowObjects, 
				column("propLongName", row -> row.getPropLongName()), 
				column("propShortName", row -> row.getPropShortName()), 
				column("originalGroup", row -> row.getOriginalGroup()), 
				column("replacementGroup", row -> row.getReplacementGroup()), 
				column("radical", row -> row.isRadical()));
	}

}
