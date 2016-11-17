package uk.ac.gla.cvr.gluetools.core.phylogenyImporter;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="phylogenyImporter")
public class PhylogenyImporter extends ModulePlugin<PhylogenyImporter> {

	public PhylogenyImporter() {
		super();
		addModulePluginCmdClass(ImportPhylogenyCommand.class);
	}

}
