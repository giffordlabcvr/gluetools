package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

@CommandClass(
		commandWords={"list", "variation-category"}, 
		description = "List variation categories", 
		docoptUsages = { "" },
		docoptOptions = { },
		metaTags = { }	
)
public class ListVariationCategoryCommand extends ModulePluginCommand<
	ListVariationCategoryCommand.ListVariationCategoryResult, WebAnalysisTool> {
	
	@Override
	protected ListVariationCategoryResult execute(CommandContext cmdContext,
			WebAnalysisTool webAnalysisTool) {
		return new ListVariationCategoryResult(webAnalysisTool.getVariationCategories());
	}

	public static class ListVariationCategoryResult extends BaseTableResult<VariationCategory> {

		public ListVariationCategoryResult(List<VariationCategory> variationCategories) {
			super("listVariationCategoryResult", variationCategories,
					column(VariationCategory.NAME, vcat -> vcat.getName()),
					column(VariationCategory.DISPLAY_NAME, vcat -> vcat.getDisplayName()),
					column(VariationCategory.DESCRIPTION, vcat -> vcat.getDescription()));
		}
		
	}


}
