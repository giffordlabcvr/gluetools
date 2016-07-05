package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class FeatureLocCountAminoAcidResult extends MapResult {

	public FeatureLocCountAminoAcidResult(String aminoAcid, int count) {
		super("featureLocCountAminoAcidResult", mapBuilder().put("aminoAcid", aminoAcid)
				.put("count", count));
	}

}
