package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;

@CommandClass(
		commandWords={"list", "labeled-codon"}, 
		docoptUsages = { "" },
		docoptOptions = { },
		description="List the labeled codons on this feature location"
)
public class FeatureLocListLabeledCodonsCommand extends FeatureLocModeCommand<FeatureLocListLabeledCodonsResult> {

	@Override
	public FeatureLocListLabeledCodonsResult execute(CommandContext cmdContext) {
		return new FeatureLocListLabeledCodonsResult(lookupFeatureLoc(cmdContext).getLabeledCodons(cmdContext));
	}

}
