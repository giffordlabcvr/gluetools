package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;

@CommandClass(
		commandWords={"amino-acid"}, 
		description = "Translate the feature location to amino acids", 
		docoptUsages = { "" },
		docoptOptions = { },
		metaTags = {}	
)
public class FeatureLocAminoAcidCommand extends FeatureLocBaseAminoAcidCommand<FeatureLocAminoAcidResult> {

	@Override
	public FeatureLocAminoAcidResult execute(CommandContext cmdContext) {
		FeatureLocation featureLoc = lookupFeatureLoc(cmdContext);
		List<LabeledAminoAcid> labeledAminoAcids = featureLocAminoAcids(cmdContext, featureLoc);
		return new FeatureLocAminoAcidResult(labeledAminoAcids);
	}

	@CompleterClass
	public static final class Completer extends AdvancedCmdCompleter {}

	
}
