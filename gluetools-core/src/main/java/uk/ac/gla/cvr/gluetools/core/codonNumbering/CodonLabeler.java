package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;

public interface CodonLabeler {

	public List<LabeledCodon> numberCodons(CommandContext cmdContext, Alignment alignment, String featureName, int ntStart, int ntEnd);
	
}
