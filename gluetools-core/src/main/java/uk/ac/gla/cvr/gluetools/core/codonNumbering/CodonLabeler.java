package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;

public interface CodonLabeler {

	public List<LabeledCodon> labelCodons(CommandContext cmdContext, FeatureLocation constrainingFeatureLoc);
	
}
