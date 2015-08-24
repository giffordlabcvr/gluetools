package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class FeatureModeCommand<R extends CommandResult> extends Command<R> {


	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}

	protected String getFeatureName() {
		return featureName;
	}


	protected static FeatureMode getFeatureMode(CommandContext cmdContext) {
		return (FeatureMode) cmdContext.peekCommandMode();
	}
}
