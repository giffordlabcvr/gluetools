package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;


@CommandClass(
	commandWords={"list", "feature-location"}, 
	docoptUsages={""},
	description="List the feature loctions for the reference") 
public class ListFeatureLocCommand extends ReferenceSequenceModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(FeatureLocation.REF_SEQ_NAME_PATH, getRefSeqName());
		return CommandUtils.runListCommand(cmdContext, FeatureLocation.class, new SelectQuery(FeatureLocation.class, exp));
	}

}
