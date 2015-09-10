package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag.FeatureMetatag;


@CommandClass( 
		commandWords={"list", "metatag"},
		docoptUsages={""},
		metaTags={},
		description="List the metatags for this feature"
	) 
public class FeatureListMetatagCommand extends FeatureModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(FeatureMetatag.FEATURE_NAME_PATH, getFeatureName());
		return CommandUtils.runListCommand(cmdContext, FeatureMetatag.class, new SelectQuery(FeatureMetatag.class, exp));
	}
	
}
