package uk.ac.gla.cvr.gluetools.core.variationscanner;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;

public abstract class BaseNucleotideVariationScanner<V extends ModulePlugin<V>, R extends VariationScanResult> extends BaseVariationScanner<V, R> {

	public abstract R scanNucleotides(Variation variation, CharSequence nucleotides, int zeroIndexNtStart);

	@Override
	public void validateVariation(Variation variation) {
		super.validateVariation(variation);
		if(variation.getTranslationFormat() != TranslationFormat.NUCLEOTIDE) {
			throw new VariationException(Code.WRONG_SCANNER_TYPE, 
					variation.getFeatureLoc().getReferenceSequence().getName(), 
					variation.getFeatureLoc().getFeature().getName(), variation.getName(), 
					BaseNucleotideVariationScanner.class.getSimpleName());
		}
	}
	
}
