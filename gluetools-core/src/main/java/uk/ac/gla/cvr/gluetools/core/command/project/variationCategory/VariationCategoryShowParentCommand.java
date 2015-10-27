package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;

@CommandClass( 
		commandWords={"show", "parent"},
		docoptUsages={""},
		description="Show the parent of this variation category"
	) 
public class VariationCategoryShowParentCommand extends VariationCategoryModeCommand<VariationCategoryShowParentCommand.VariationCategoryShowParentResult> {

	@Override
	public VariationCategoryShowParentResult execute(CommandContext cmdContext) {
		VariationCategory variationCategory = lookupVariationCategory(cmdContext);
		String parentName = null;
		VariationCategory parent = variationCategory.getParent();
		if(parent != null) {
			parentName = parent.getName();
		}
		return new VariationCategoryShowParentResult(parentName);
	}
	
	public class VariationCategoryShowParentResult extends MapResult {

		public VariationCategoryShowParentResult(String parentName) {
			super("variationCategoryShowParentResult", mapBuilder().put(VariationCategory.PARENT_NAME_PATH, parentName));
		}


	}


}
