package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;


@CommandClass( 
		commandWords={"list", "variation"},
		docoptUsages={""},
		description="List feature variations"
	) 
public class ListVariationCommand extends FeatureLocModeCommand<ListResult> {
	
	@Override
	public ListResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, Variation.class, new SelectQuery(Variation.class, 
				ExpressionFactory
					.matchExp(Variation.FEATURE_NAME_PATH, getFeatureName())
					.andExp(ExpressionFactory
							.matchExp(Variation.REF_SEQ_NAME_PATH, getRefSeqName())
				)));
	}
	
}
