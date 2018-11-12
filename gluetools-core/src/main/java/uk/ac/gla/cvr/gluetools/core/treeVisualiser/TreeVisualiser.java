package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName = "treeVisualiser", 
description="Renders GLUE command document tree into a command document form with many visualisation calculations already performed")
public class TreeVisualiser extends ModulePlugin<TreeVisualiser> {

	public TreeVisualiser() {
		super();
		registerModulePluginCmdClass(VisualiseTreeDocumentCommand.class);
	}

}
