package uk.ac.gla.cvr.gluetools.core.replacementClassification;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="hanada2006ReplacementClassifier",
description="Classification of single amino acid replacements based on 3 alternative classification schemes")
public class Hanada2006ReplacementClassifier extends ModulePlugin<Hanada2006ReplacementClassifier>{

	public Hanada2006ReplacementClassifier() {
		super();
		registerModulePluginCmdClass(Hanada2006ClassifyReplacementCommand.class);
	}

}
