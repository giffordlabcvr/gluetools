package uk.ac.gla.cvr.gluetools.core.command.project.alignment.feature;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;


@CommandClass(
	commandWords={"list", "segment"}, 
	docoptUsages={""},
	description="List the reference sequence segments") 
public class ListFeatureSegmentCommand extends FeatureModeCommand {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, FeatureSegment.class, new SelectQuery(FeatureSegment.class));
	}

}
