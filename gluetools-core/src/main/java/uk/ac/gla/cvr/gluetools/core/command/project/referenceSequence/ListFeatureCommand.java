package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;


@CommandClass(
	commandWords={"list", "feature"}, 
	docoptUsages={""},
	description="List the features of the reference sequence") 
public class ListFeatureCommand extends ReferenceSequenceModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Feature.REF_SEQ_NAME_PATH, getRefSeqName());
		return CommandUtils.runListCommand(cmdContext, Feature.class, new SelectQuery(Feature.class, exp));
	}

}
