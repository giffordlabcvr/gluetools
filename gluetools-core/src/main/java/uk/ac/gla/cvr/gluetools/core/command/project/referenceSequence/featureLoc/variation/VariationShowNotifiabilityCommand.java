package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation.NotifiabilityLevel;

@CommandClass( 
		commandWords={"show","notifiability"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Show the variation's notifiability level") 
public class VariationShowNotifiabilityCommand extends VariationModeCommand<VariationShowNotifiabilityCommand.VariationShowNotifiabilityResult> {

	@Override
	public VariationShowNotifiabilityResult execute(CommandContext cmdContext) {
		return new VariationShowNotifiabilityResult(lookupVariation(cmdContext).getNotifiabilityLevel());
	}

	public class VariationShowNotifiabilityResult extends MapResult {

		public VariationShowNotifiabilityResult(NotifiabilityLevel notifiabilityLevel) {
			super("variationShowNotifiabilityResult", mapBuilder()
					.put(Variation.NOTIFIABILITY_PROPERTY, notifiabilityLevel.name())
					);
		}

		
	}

}

