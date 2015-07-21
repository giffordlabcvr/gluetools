package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

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
	description="List the features of the alignment") 
public class ListFeatureCommand extends AlignmentModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		Expression exp = ExpressionFactory.matchExp(Feature.ALIGNMENT_NAME_PATH, getAlignmentName());
		return CommandUtils.runListCommand(cmdContext, Feature.class, new SelectQuery(Feature.class, exp));
	}

}
