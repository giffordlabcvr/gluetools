package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.mafft.MafftRunner;

public class MafftAligner extends Aligner<MafftAligner.MafftAlignerResult, MafftAligner> implements SupportsExtendUnconstrained {

	private MafftRunner mafftRunner = new MafftRunner();
	
	public MafftAligner() {
		super();
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element mafftRunnerElem = PluginUtils.findConfigElement(configElem, "mafftRunner");
		if(mafftRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, mafftRunnerElem, mafftRunner);
		}
	}

	
	public static class MafftAlignerResult extends Aligner.AlignerResult {
		public MafftAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("mafftAlignerResult", fastaIdToAlignedSegments);
		}
	}
}
