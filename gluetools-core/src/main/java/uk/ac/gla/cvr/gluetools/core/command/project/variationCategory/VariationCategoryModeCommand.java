package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class VariationCategoryModeCommand<R extends CommandResult> extends ProjectModeCommand<R> {


	private String vcatName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		vcatName = PluginUtils.configureStringProperty(configElem, "vcatName", true);
	}

	protected String getVcatName() {
		return vcatName;
	}


	protected static VariationCategoryMode getVariationCategoryMode(CommandContext cmdContext) {
		return (VariationCategoryMode) cmdContext.peekCommandMode();
	}


	protected VariationCategory lookupVariationCategory(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext, VariationCategory.class, 
				VariationCategory.pkMap(getVcatName()));
	}


}
