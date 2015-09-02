package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;

@CommandClass( 
		commandWords={"show", "parent"},
		docoptUsages={""},
		description="Show the parent of this feature"
	) 
public class FeatureShowParentCommand extends FeatureModeCommand<FeatureShowParentCommand.FeatureShowParentResult> {

	@Override
	public FeatureShowParentResult execute(CommandContext cmdContext) {
		Feature feature = lookupFeature(cmdContext);
		String parentName = null;
		Feature parent = feature.getParent();
		if(parent != null) {
			parentName = parent.getName();
		}
		return new FeatureShowParentResult(parentName);
	}
	
	public class FeatureShowParentResult extends MapResult {

		public FeatureShowParentResult(String parentName) {
			super("featureShowParentResult", mapBuilder().put(Feature.PARENT_NAME_PATH, parentName));
		}


	}


}
