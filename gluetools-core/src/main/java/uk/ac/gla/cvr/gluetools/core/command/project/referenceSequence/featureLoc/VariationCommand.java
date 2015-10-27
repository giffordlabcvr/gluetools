package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.EnterModeCommandClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation.VariationMode;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
	commandWords={"variation"},
	docoptUsages={"<variationName>"},
	description="Enter command mode for a feature variation")
@EnterModeCommandClass(
		commandModeClass = VariationMode.class)
public class VariationCommand extends FeatureLocModeCommand<OkResult>  {

	public static final String VARIATION_NAME = "variationName";
	private String variationName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		variationName = PluginUtils.configureStringProperty(configElem, VARIATION_NAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		
		Variation variation = GlueDataObject.lookup(cmdContext, Variation.class, 
				Variation.pkMap(getRefSeqName(), getFeatureName(), variationName));
		cmdContext.pushCommandMode(new VariationMode(
				getFeatureLocMode(cmdContext).getProject(), this, getRefSeqName(), getFeatureName(), variation.getName()));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends VariationNameCompleter {}

}
