package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.QueryGenotypingResult;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.IMaxLikelihoodPlacerResult;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;

public abstract class BaseGenotypePlacerResultCommand<P extends BaseGenotyper<P>> extends AbstractGenotypeCommand<P> {

	protected final CommandResult executeOnPlacerResultDocument(CommandContext cmdContext, P maxLikelihoodGenotyper, CommandDocument placerResultCmdDoc) {
		IMaxLikelihoodPlacerResult placerResult = IMaxLikelihoodPlacerResult.fromCommandDocument(placerResultCmdDoc);
	
		MaxLikelihoodPlacer placer = maxLikelihoodGenotyper.resolvePlacer(cmdContext);
		PhyloTree glueProjectPhyloTree = placer.constructGlueProjectPhyloTree(cmdContext);
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = 
				MaxLikelihoodPlacer.generateEdgeIndexToPhyloBranch(placerResult.getLabelledPhyloTree(), glueProjectPhyloTree);
		Map<String, QueryGenotypingResult> genotypeResults = maxLikelihoodGenotyper.genotype(cmdContext, glueProjectPhyloTree, edgeIndexToPhyloBranch, placerResult.getQueryResults());
		return formResult(maxLikelihoodGenotyper, genotypeResults);
	}


}
