package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="visualisationUtility", 
description="Renders project data in forms appropriate for visualisation")
public class VisualisationUtility extends ModulePlugin<VisualisationUtility> {

	public VisualisationUtility() {
		super();
		registerModulePluginCmdClass(VisualiseFeatureCommand.class);
	}

	
	
}
