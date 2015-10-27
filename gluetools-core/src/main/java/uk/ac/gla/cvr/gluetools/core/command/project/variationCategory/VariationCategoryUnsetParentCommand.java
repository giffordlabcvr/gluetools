package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;

@CommandClass( 
		commandWords={"unset", "parent"},
		docoptUsages={""},
		metaTags={CmdMeta.updatesDatabase},
		description="Unset the parent of this variation category"
	) 
public class VariationCategoryUnsetParentCommand extends VariationCategoryModeCommand<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		VariationCategory variationCategory = lookupVariationCategory(cmdContext);
		variationCategory.setParent(null);
		cmdContext.commit();
		return new OkResult();
	}

}
