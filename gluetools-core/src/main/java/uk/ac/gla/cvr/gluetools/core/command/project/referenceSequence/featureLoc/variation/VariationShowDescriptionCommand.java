package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@CommandClass( 
		commandWords={"show","description"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Show the variation's description") 
public class VariationShowDescriptionCommand extends VariationModeCommand<VariationShowDescriptionCommand.VariationShowDescriptionResult> {

	@Override
	public VariationShowDescriptionResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		return new VariationShowDescriptionResult(variation.getDescription());
	}

	public class VariationShowDescriptionResult extends MapResult {

		public VariationShowDescriptionResult(String description) {
			super("variationShowDescriptionResult", mapBuilder()
					.put(Variation.DESCRIPTION_PROPERTY, description)
					);
		}

		
	}

}

