package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;

@CommandClass( 
		commandWords={"validate"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Validate that a feature location is correctly defined.", 
		furtherHelp="Also validates any variations of this feature location") 
public class FeatureLocValidateCommand extends FeatureLocModeCommand<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		lookupFeatureLoc(cmdContext).validate(cmdContext);
		return new OkResult();
	}
	
}

