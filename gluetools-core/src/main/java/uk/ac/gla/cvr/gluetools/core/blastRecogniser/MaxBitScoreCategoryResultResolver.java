package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;

@PluginClass(elemName = "maxBitScoreCategoryResultResolver")
public class MaxBitScoreCategoryResultResolver extends CategoryResultResolver {

	private Double minDifference;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.minDifference = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "minDifference", false)).orElse(0.0);
	}

	@Override
	public int compare(RecognitionCategoryResult recCatResult1,
			List<BlastHsp> hsps1, RecognitionCategoryResult recCatResult2,
			List<BlastHsp> hsps2) {
		double maxBitScore1 = hsps1.stream().mapToDouble(BlastHsp::getBitScore).max().getAsDouble();
		double maxBitScore2 = hsps2.stream().mapToDouble(BlastHsp::getBitScore).max().getAsDouble();
		if(maxBitScore2 > maxBitScore1 + minDifference) {
			return -1;
		}
		if(maxBitScore1 > maxBitScore2 + minDifference) {
			return 1;
		}
		return 0;
	}

}
