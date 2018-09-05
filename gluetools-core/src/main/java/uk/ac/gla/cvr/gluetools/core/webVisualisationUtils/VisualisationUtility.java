package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="visualisationUtility", 
description="Renders project data in forms appropriate for visualisation")
public class VisualisationUtility extends ModulePlugin<VisualisationUtility> {

	private static final String LINKING_ALIGNMENT_NAME = "linkingAlignmentName";
	private String linkingAlignmentName;
	
	public VisualisationUtility() {
		super();
		registerModulePluginCmdClass(VisualiseFeatureCommand.class);
		addSimplePropertyName(LINKING_ALIGNMENT_NAME);
	}

	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.linkingAlignmentName = PluginUtils.configureStringProperty(configElem, LINKING_ALIGNMENT_NAME, true);
	}



	public String getLinkingAlignmentName() {
		return linkingAlignmentName;
	}
	
}
