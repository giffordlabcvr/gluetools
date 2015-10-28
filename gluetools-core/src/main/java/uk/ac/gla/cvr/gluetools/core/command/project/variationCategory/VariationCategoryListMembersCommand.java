package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.variationCategory.VariationCategoryListMembersCommand.VariationCategoryListMembersResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.datamodel.vcatMembership.VcatMembership;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"list","members"}, 
		docoptUsages={"[-r]"},
		docoptOptions={"-r, --recursive  Include members of descendent categories"},
		metaTags={},
		description="List the variations in this category", 
		furtherHelp="") 
public class VariationCategoryListMembersCommand extends VariationCategoryModeCommand<VariationCategoryListMembersResult> {

	public static final String RECURSIVE = "recursive";

	private boolean recursive;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		recursive = PluginUtils.configureBooleanProperty(configElem, RECURSIVE, true);
	}

	@Override
	public VariationCategoryListMembersResult execute(CommandContext cmdContext) {
		VariationCategory variationCategory = lookupVariationCategory(cmdContext);
		List<VariationCategory> categories;
		if(recursive) {
			categories = new ArrayList<VariationCategory>(variationCategory.getDescendents());
			categories.add(variationCategory);
		} else {
			categories = Collections.singletonList(variationCategory);
		}
		List<Map<String, Object>> rowData = new ArrayList<Map<String,Object>>();
		for(VariationCategory vcat: categories) {
			for(VcatMembership vcatMembership: vcat.getMemberships()) {
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				map.put(VcatMembership.VARIATION_REFSEQ_NAME_PATH, vcatMembership.getVariation().getFeatureLoc().getReferenceSequence().getName());
				map.put(VcatMembership.VARIATION_FEATURE_NAME_PATH, vcatMembership.getVariation().getFeatureLoc().getFeature().getName()); 
				map.put(VcatMembership.VARIATION_NAME_PATH, vcatMembership.getVariation().getName());
				map.put(VcatMembership.CATEGORY_NAME_PATH, vcatMembership.getCategory().getName());
				rowData.add(map);
			}
		}
		return new VariationCategoryListMembersResult(rowData);
	}

	public static class VariationCategoryListMembersResult extends TableResult {

		public VariationCategoryListMembersResult(List<Map<String, Object>> rowData) {
			super("variationCategoryListMembersResult", 
					Arrays.asList(
							VcatMembership.VARIATION_REFSEQ_NAME_PATH, 
							VcatMembership.VARIATION_FEATURE_NAME_PATH, 
							VcatMembership.VARIATION_NAME_PATH, 
							VcatMembership.CATEGORY_NAME_PATH), rowData);
		}
		
		
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}
	
}
