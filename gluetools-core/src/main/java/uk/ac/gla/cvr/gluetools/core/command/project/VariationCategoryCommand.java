package uk.ac.gla.cvr.gluetools.core.command.project;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.variationCategory.VariationCategoryMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationCategory.VariationCategory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"variation-category"},
	docoptUsages={"<vcatName>"},
	description="Enter command mode for a variation category")
@EnterModeCommandClass(
		commandModeClass = VariationCategoryMode.class)
public class VariationCategoryCommand extends ProjectModeCommand<OkResult>  {

	public static final String VCAT_NAME = "vcatName";
	private String vcatName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		vcatName = PluginUtils.configureStringProperty(configElem, VCAT_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		VariationCategory variationCategory = GlueDataObject.lookup(cmdContext, VariationCategory.class, VariationCategory.pkMap(vcatName));
		cmdContext.pushCommandMode(new VariationCategoryMode(getProjectMode(cmdContext).getProject(), this, variationCategory.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends VariationCategoryNameCompleter {}	

}
