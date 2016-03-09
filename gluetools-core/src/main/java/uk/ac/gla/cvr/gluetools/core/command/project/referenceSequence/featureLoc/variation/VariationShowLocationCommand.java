package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@CommandClass( 
		commandWords={"show", "location"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Show the variation's location") 
public class VariationShowLocationCommand extends VariationModeCommand<VariationShowLocationCommand.VariationShowLocationResult> {

	@Override
	public VariationShowLocationResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		return new VariationShowLocationResult(variation.getRefStart(), variation.getRefEnd());
	}

	public class VariationShowLocationResult extends MapResult {

		public VariationShowLocationResult(int refStart, int refEnd) {
			super("variationShowLocationResult", mapBuilder()
					.put(Variation.REF_START_PROPERTY, refStart)
					.put(Variation.REF_END_PROPERTY, refEnd)
					);
		}

		
	}

}
