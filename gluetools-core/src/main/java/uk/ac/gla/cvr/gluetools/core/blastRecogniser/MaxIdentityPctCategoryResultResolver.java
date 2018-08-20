package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;

@PluginClass(elemName = "maxIdentityPctCategoryResultResolver")
public class MaxIdentityPctCategoryResultResolver extends CategoryResultResolver {

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
		double maxIdentityPct1 = hsps1.stream().mapToDouble(BlastHsp::getIdentityPct).max().getAsDouble();
		double maxIdentityPct2 = hsps2.stream().mapToDouble(BlastHsp::getIdentityPct).max().getAsDouble();
		if(maxIdentityPct2 > maxIdentityPct1 + minDifference) {
			return -1;
		}
		if(maxIdentityPct1 > maxIdentityPct2 + minDifference) {
			return 1;
		}
		return 0;
	}

}
