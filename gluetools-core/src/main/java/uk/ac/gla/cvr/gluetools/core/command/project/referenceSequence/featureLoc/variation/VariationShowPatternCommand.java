package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.transcription.TranslationFormat;

@CommandClass( 
		commandWords={"show","pattern"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Show the variation's pattern") 
public class VariationShowPatternCommand extends VariationModeCommand<VariationShowPatternCommand.VariationShowPatternResult> {

	@Override
	public VariationShowPatternResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		return new VariationShowPatternResult(variation.getTranslationFormat(), variation.getRegex());
	}

	public class VariationShowPatternResult extends MapResult {

		public VariationShowPatternResult(TranslationFormat transcriptionType, String regex) {
			super("variationShowPatternResult", mapBuilder()
					.put(Variation.TRANSLATION_TYPE_PROPERTY, transcriptionType.name())
					.put(Variation.REGEX_PROPERTY, regex)
					);
		}

		
	}

}

