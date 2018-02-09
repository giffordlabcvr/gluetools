package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="webVisualisationUtility", 
description="Renders project data in forms appropriate for visualisation on the web")
public class WebVisualisationUtility extends ModulePlugin<WebVisualisationUtility> {

	public WebVisualisationUtility() {
		super();
		registerModulePluginCmdClass(VisualiseFeatureCommand.class);
	}

	
	
}
