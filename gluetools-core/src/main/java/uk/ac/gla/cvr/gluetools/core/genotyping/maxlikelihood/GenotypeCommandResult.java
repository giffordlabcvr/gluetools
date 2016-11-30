package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class GenotypeCommandResult extends BaseTableResult<GenotypeResult> {

	public GenotypeCommandResult(List<GenotypeResult> genotypeResults) {
		super("genotypeCommandResult", genotypeResults,
				column("sequenceName", gResult -> gResult.getSequenceName()));
	}

}
