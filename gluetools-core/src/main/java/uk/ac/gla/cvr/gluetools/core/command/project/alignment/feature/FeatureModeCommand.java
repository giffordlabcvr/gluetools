package uk.ac.gla.cvr.gluetools.core.command.project.alignment.feature;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public abstract class FeatureModeCommand extends AlignmentModeCommand {


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
