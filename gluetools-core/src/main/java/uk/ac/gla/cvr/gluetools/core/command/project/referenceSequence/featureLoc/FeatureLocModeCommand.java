package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.ReferenceSequenceModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class FeatureLocModeCommand<R extends CommandResult> extends ReferenceSequenceModeCommand<R> {


	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}

	protected String getFeatureName() {
		return featureName;
	}

}