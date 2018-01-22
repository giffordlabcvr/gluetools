/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
					column(VariationCategory.DESCRIPTION, vcat -> vcat.getDescription()),
					column(VariationCategory.SELECTED_BY_DEFAULT, vcat -> vcat.getSelectedByDefault()),
					column(VariationCategory.OBJECT_RENDERER_MODULE, vcat -> vcat.getObjectRendererModule()));
		}
		
	}


}
