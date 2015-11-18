package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;


@CommandClass(
	commandWords={"list", "variation-category"}, 
	docoptUsages={""},
	description="List variation categories") 
public class ListVariationCategoryCommand extends ProjectModeCommand<ListVariationCategoryCommand.ListVariationCategoryResult> {

	@Override
	public ListVariationCategoryResult execute(CommandContext cmdContext) {
		List<VariationCategory> variationCategories = GlueDataObject.query(cmdContext, VariationCategory.class, new SelectQuery(VariationCategory.class));
		return new ListVariationCategoryResult(variationCategories);
	}

	
	public static class ListVariationCategoryResult extends ListResult {

		protected ListVariationCategoryResult(List<VariationCategory> results) {
			super(VariationCategory.class, results, 
					Stream.concat(ListResult.propertyPaths(VariationCategory.class).stream(), 
							Arrays.asList(VariationCategory.INHERITED_NOTIFIABILITY).stream()).collect(Collectors.toList()), 
							new HeaderResolver());
		}

		public static class HeaderResolver extends MapResult.DefaultResolveHeaderFunction<VariationCategory> {

			@Override
			public Object apply(VariationCategory vcat, String header) {
				if(header.equals(VariationCategory.INHERITED_NOTIFIABILITY)) {
					return vcat.getInheritedNotifiability().name();
				}
				return super.apply(vcat, header);
			}
			
		}

	}

}
