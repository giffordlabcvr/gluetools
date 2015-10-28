package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

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
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationListCategoryCommand.VariationListCategoryResult;
import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.datamodel.vcatMembership.VcatMembership;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"list","category"}, 
		docoptUsages={"[-a]"},
		docoptOptions={"-a, --ancestors  Include ancestor categories"},
		metaTags={},
		description="List the categories this variation is a member of", 
		furtherHelp="") 
public class VariationListCategoryCommand extends VariationModeCommand<VariationListCategoryResult> {

	public static final String ANCESTORS = "ancestors";

	private boolean ancestors;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		ancestors = PluginUtils.configureBooleanProperty(configElem, ANCESTORS, true);
	}

	@Override
	public VariationListCategoryResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		List<Map<String, Object>> rowData = new ArrayList<Map<String,Object>>();
		for(VcatMembership vcatMembership: variation.getVcatMemberships()) {
			VariationCategory directCategory = vcatMembership.getCategory();
			List<VariationCategory> categories;
			if(ancestors) {
				categories = new ArrayList<VariationCategory>(directCategory.getAncestors());
			} else {
				categories = Collections.singletonList(directCategory);
			}
			for(VariationCategory cat: categories) {
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				map.put(VcatMembership.CATEGORY_NAME_PATH, cat.getName());
				map.put(VcatMembership.CATEGORY_DESCRIPTION_PATH, cat.getDescription());
				rowData.add(map);
			}
		}
		return new VariationListCategoryResult(rowData);
	}

	public static class VariationListCategoryResult extends TableResult {

		public VariationListCategoryResult(List<Map<String, Object>> rowData) {
			super("variationListCategoryResult", 
					Arrays.asList(
							VcatMembership.CATEGORY_NAME_PATH, 
							VcatMembership.CATEGORY_DESCRIPTION_PATH), rowData);
		}
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {}
	
}
