package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class VariationModeCommand<R extends CommandResult> extends FeatureLocModeCommand<R> {


	private String variationName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		variationName = PluginUtils.configureStringProperty(configElem, "variationName", true);
	}

	protected String getVariationName() {
		return variationName;
	}


	protected static VariationMode getVariationMode(CommandContext cmdContext) {
		return (VariationMode) cmdContext.peekCommandMode();
	}


	protected Variation lookupVariation(CommandContext cmdContext) {
		return GlueDataObject.lookup(cmdContext.getObjectContext(), Variation.class, 
				Variation.pkMap(getRefSeqName(), getFeatureName(), getVariationName()));
	}


}
