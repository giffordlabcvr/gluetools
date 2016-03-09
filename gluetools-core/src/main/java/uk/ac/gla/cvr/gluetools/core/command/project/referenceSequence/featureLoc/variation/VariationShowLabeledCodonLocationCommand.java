package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import gnu.trove.map.TIntObjectMap;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

@CommandClass( 
		commandWords={"show", "labeled-codon", "location"}, 
		docoptUsages={""},
		docoptOptions={},
		metaTags={},
		description="Show the variation's labeled codon location") 
public class VariationShowLabeledCodonLocationCommand extends VariationModeCommand<VariationShowLabeledCodonLocationCommand.VariationShowLabeledCodonLocationResult> {

	@Override
	public VariationShowLabeledCodonLocationResult execute(CommandContext cmdContext) {
		Variation variation = lookupVariation(cmdContext);
		TIntObjectMap<LabeledCodon> refNtToLabeledCodon = variation.getFeatureLoc().getRefNtToLabeledCodon(cmdContext);
		LabeledCodon startCodon = refNtToLabeledCodon.get(variation.getRefStart());
		String startLC = startCodon == null? null : startCodon.getLabel();
		LabeledCodon endCodon = refNtToLabeledCodon.get(variation.getRefEnd()-2);
		String endLC = endCodon == null? null : endCodon.getLabel();
		return new VariationShowLabeledCodonLocationResult(startLC, endLC);
	}

	public class VariationShowLabeledCodonLocationResult extends MapResult {

		public VariationShowLabeledCodonLocationResult(String startLC, String endLC) {
			super("variationShowLocationResult", mapBuilder()
					.put("startLabeledCodon", startLC)
					.put("endLabeledCodon", endLC)
					);
		}

		
	}

}
