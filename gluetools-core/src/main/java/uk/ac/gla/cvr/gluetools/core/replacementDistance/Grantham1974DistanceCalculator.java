package uk.ac.gla.cvr.gluetools.core.replacementDistance;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="grantham1974DistanceCalculator",
description="Calculation of amino acid distance based on Grantham, 1974")
public class Grantham1974DistanceCalculator extends ModulePlugin<Grantham1974DistanceCalculator>{

	public Grantham1974DistanceCalculator() {
		super();
		registerModulePluginCmdClass(Grantham1974DistanceCommand.class);
	}

}
