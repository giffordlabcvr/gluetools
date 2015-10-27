package uk.ac.gla.cvr.gluetools.core.command.project;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;


@CommandClass(
	commandWords={"list", "variation-category"}, 
	docoptUsages={""},
	description="List variation categories") 
public class ListVariationCategoryCommand extends ProjectModeCommand<ListResult> {

	@Override
	public ListResult execute(CommandContext cmdContext) {
		return CommandUtils.runListCommand(cmdContext, VariationCategory.class, new SelectQuery(VariationCategory.class));
	}

}
