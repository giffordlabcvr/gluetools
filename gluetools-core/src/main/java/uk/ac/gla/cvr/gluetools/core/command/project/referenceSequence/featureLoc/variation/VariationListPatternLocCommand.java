package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@CommandClass( 
		commandWords={"list", "pattern-location"}, 
		docoptUsages={""},
		metaTags={},
		description="List the pattern-locations of the variation, with nucleotide coordinates") 

public class VariationListPatternLocCommand extends VariationModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		
		List<PatternLocation> patternLocs = variation.getPatternLocs();
		
		return new ListResult(cmdContext, PatternLocation.class, patternLocs);
		
	}
}
