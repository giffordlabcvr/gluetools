package uk.ac.gla.cvr.gluetools.core.variationscanner;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationFormat;

public abstract class BaseAminoAcidVariationScanner<V extends ModulePlugin<V>, R extends VariationScanResult> extends BaseVariationScanner<V, R> {

	public abstract R scanAminoAcids(CommandContext cmdContext, Variation variation, NtQueryAlignedSegment ntQaSegCdnAligned, String fullAminoAcidTranslation);

	@Override
	public void validateVariation(Variation variation) {
		super.validateVariation(variation);
		if(variation.getTranslationFormat() != TranslationFormat.AMINO_ACID) {
			throw new VariationException(Code.WRONG_SCANNER_TYPE, 
					variation.getFeatureLoc().getReferenceSequence().getName(), 
					variation.getFeatureLoc().getFeature().getName(), variation.getName(), 
					BaseAminoAcidVariationScanner.class.getSimpleName());
		}
	}

}
