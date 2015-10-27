package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory.NotifiabilityLevel;

@CommandClass( 
		commandWords={"show","notifiability"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Show the variation category's notifiability level") 
public class VariationCategoryShowNotifiabilityCommand extends VariationCategoryModeCommand<VariationCategoryShowNotifiabilityCommand.VariationCategoryShowNotifiabilityResult> {

	@Override
	public VariationCategoryShowNotifiabilityResult execute(CommandContext cmdContext) {
		return new VariationCategoryShowNotifiabilityResult(lookupVariationCategory(cmdContext).getNotifiabilityLevel());
	}

	public class VariationCategoryShowNotifiabilityResult extends MapResult {

		public VariationCategoryShowNotifiabilityResult(NotifiabilityLevel notifiabilityLevel) {
			super("variationCategoryShowNotifiabilityResult", mapBuilder()
					.put(VariationCategory.NOTIFIABILITY_PROPERTY, notifiabilityLevel.name())
			);
		}

		
	}

}

