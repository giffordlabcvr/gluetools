package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;

@PluginClass(elemName = "categoryNameResultResolver")
public class CategoryNameResultResolver extends CategoryResultResolver {

	@Override
	public int compare(RecognitionCategoryResult recCatResult1,
			List<BlastHsp> hsps1, RecognitionCategoryResult recCatResult2,
			List<BlastHsp> hsps2) {
		return recCatResult2.getCategoryId().compareTo(recCatResult1.getCategoryId());
	}

}
