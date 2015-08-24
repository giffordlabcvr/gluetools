package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;


@CommandClass(
	commandWords={"list", "feature"}, 
	docoptUsages={""},
	description="List genome features") 
public class ListFeatureCommand extends ProjectModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, Feature.class, new SelectQuery(Feature.class));
	}

}
