package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;

@CommandClass( 
		commandWords={"validate"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Validate that a feature is correctly defined.", 
		furtherHelp="") 
public class FeatureValidateCommand extends FeatureModeCommand<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		lookupFeature(cmdContext).validate(cmdContext);
		return new OkResult();
	}
	
}

