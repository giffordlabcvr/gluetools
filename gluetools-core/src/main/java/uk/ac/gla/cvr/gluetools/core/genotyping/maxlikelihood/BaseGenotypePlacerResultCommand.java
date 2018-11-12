package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacerResult;

public abstract class BaseGenotypePlacerResultCommand extends AbstractGenotypeCommand {

	protected final CommandResult executeOnPlacerResultDocument(CommandContext cmdContext, MaxLikelihoodGenotyper maxLikelihoodGenotyper, CommandDocument placerResultCmdDoc) {
		MaxLikelihoodPlacerResult placerResult = PojoDocumentUtils.commandObjectToPojo(placerResultCmdDoc, MaxLikelihoodPlacerResult.class);
	
		MaxLikelihoodPlacer placer = maxLikelihoodGenotyper.resolvePlacer(cmdContext);
		PhyloTree glueProjectPhyloTree = placer.constructGlueProjectPhyloTree(cmdContext);
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = 
				MaxLikelihoodPlacer.generateEdgeIndexToPhyloBranch(placerResult.getLabelledPhyloTree(), glueProjectPhyloTree);
		Map<String, QueryGenotypingResult> genotypeResults = maxLikelihoodGenotyper.genotype(cmdContext, glueProjectPhyloTree, edgeIndexToPhyloBranch, placerResult.singleQueryResult);
		return formResult(maxLikelihoodGenotyper, genotypeResults);
	}


}
