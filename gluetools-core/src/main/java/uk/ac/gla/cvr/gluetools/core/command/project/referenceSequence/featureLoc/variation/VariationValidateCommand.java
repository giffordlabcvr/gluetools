package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;

@CommandClass( 
		commandWords={"validate"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Validate that a variation is correctly defined.") 
public class VariationValidateCommand extends VariationModeCommand<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		lookupVariation(cmdContext).validate(cmdContext);
		return new OkResult();
	}
	
}

