package uk.ac.gla.cvr.gluetools.core.variationscanner;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;

public abstract class BaseVariationScanner<V extends ModulePlugin<V>, R extends VariationScanResult> extends ModulePlugin<V> {
	
	public void validateVariation(Variation variation) {}
	
}
