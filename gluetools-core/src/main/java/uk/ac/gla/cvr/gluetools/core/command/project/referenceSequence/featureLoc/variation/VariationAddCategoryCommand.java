package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.datamodel.vcatMembership.VcatMembership;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"add","category"}, 
		docoptUsages={"<vcatName>"},
		metaTags={CmdMeta.updatesDatabase},
		description="", 
		furtherHelp="Add variation as a member of a variation category") 
public class VariationAddCategoryCommand extends VariationModeCommand<CreateResult> {

	public static final String VCAT_NAME = "vcatName";

	private String vcatName;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		vcatName = PluginUtils.configureStringProperty(configElem, VCAT_NAME, true);
		
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		VariationCategory vcat = GlueDataObject.lookup(cmdContext, VariationCategory.class, VariationCategory.pkMap(vcatName));
		VcatMembership vcatMembership = GlueDataObject.create(cmdContext, VcatMembership.class, 
				VcatMembership.pkMap(getRefSeqName(), getFeatureName(), getVariationName(), vcat.getName()), false);
		vcatMembership.setCategory(vcat);
		vcatMembership.setVariation(lookupVariation(cmdContext));
		cmdContext.commit();
		return new CreateResult(VcatMembership.class, 1);
	
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup(VCAT_NAME, VariationCategory.class, VariationCategory.NAME_PROPERTY);
		}
		
	}

}
