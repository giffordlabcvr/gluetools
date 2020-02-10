package uk.ac.gla.cvr.gluetools.core.replacementDistance;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="miyata1979DistanceCalculator",
description="Calculation of amino acid distance based on Miyata, 1979")
public class Miyata1979DistanceCalculator extends ModulePlugin<Miyata1979DistanceCalculator>{

	public Miyata1979DistanceCalculator() {
		super();
		registerModulePluginCmdClass(Miyata1979DistanceCommand.class);
	}

}
