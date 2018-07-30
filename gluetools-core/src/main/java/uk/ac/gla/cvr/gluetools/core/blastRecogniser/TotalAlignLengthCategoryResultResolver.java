package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;

@PluginClass(elemName = "totalAlignLengthCategoryResultResolver")
public class TotalAlignLengthCategoryResultResolver extends CategoryResultResolver {

	private Integer minDifference;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.minDifference = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "minDifference", false)).orElse(0);
	}

	@Override
	public int compare(RecognitionCategoryResult recCatResult1,
			List<BlastHsp> hsps1, RecognitionCategoryResult recCatResult2,
			List<BlastHsp> hsps2) {
		int totalAlignLen1 = hsps1.stream().mapToInt(BlastHsp::getAlignLen).sum();
		int totalAlignLen2 = hsps2.stream().mapToInt(BlastHsp::getAlignLen).sum();
		if(totalAlignLen2 > totalAlignLen1 + minDifference) {
			return -1;
		}
		if(totalAlignLen1 > totalAlignLen2 + minDifference) {
			return 1;
		}
		return 0;
	}

}
