package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;

public abstract class AbstractGenotypeCommand extends ModulePluginCommand<GenotypeCommandResult, MaxLikelihoodGenotyper> {

	protected GenotypeCommandResult generateCommandResults(Map<String, GenotypeResult> genotypeResults) {
		return new GenotypeCommandResult(new ArrayList<GenotypeResult>(genotypeResults.values()));
	}

}
