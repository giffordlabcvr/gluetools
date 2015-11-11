package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"delete", "variation-category"}, 
	docoptUsages={"<vcatName>"},
	metaTags={CmdMeta.updatesDatabase},
	description="Delete a variation category") 
public class DeleteVariationCategoryCommand extends ProjectModeCommand<DeleteResult> {

	private String vcatName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		vcatName = PluginUtils.configureStringProperty(configElem, "vcatName", true);
	}

	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		
		DeleteResult result = 
				GlueDataObject.delete(cmdContext, VariationCategory.class, VariationCategory.pkMap(vcatName), true);
		cmdContext.commit();
		return result;
	}

	@CompleterClass
	public static class Completer extends VariationCategoryNameCompleter {}

}
