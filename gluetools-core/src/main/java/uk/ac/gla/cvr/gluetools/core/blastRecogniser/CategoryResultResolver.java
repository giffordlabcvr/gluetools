package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;

public abstract class CategoryResultResolver implements Plugin {

	public abstract int compare(RecognitionCategoryResult recCatResult1,
					List<BlastHsp> hsps1, int totalAlignLen1, RecognitionCategoryResult recCatResult2,
					List<BlastHsp> hsps2, int totalAlignLen2);

}
