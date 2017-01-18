package uk.ac.gla.cvr.gluetools.custom.epitopeRasOverlap;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="epitopeRasOverlap")
public class EpitopeRasOverlap extends ModulePlugin<EpitopeRasOverlap> {

	public EpitopeRasOverlap() {
		super();
		addModulePluginCmdClass(ComputeOverlappingCommand.class);
	
	}

}
