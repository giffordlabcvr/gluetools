package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;

@CommandClass( 
		commandWords={"unset", "parent"},
		docoptUsages={""},
		metaTags={CmdMeta.updatesDatabase},
		description="Unset the parent of this feature"
	) 
public class FeatureUnsetParentCommand extends FeatureModeCommand<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		feature.setParent(null);
		cmdContext.commit();
		return new OkResult();
	}

}
