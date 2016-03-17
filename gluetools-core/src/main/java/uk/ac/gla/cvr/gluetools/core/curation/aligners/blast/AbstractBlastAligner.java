package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAligner.BlastAlignCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAligner.BlastFileAlignCommand;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHspFilter;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;

public abstract class AbstractBlastAligner<R extends Aligner.AlignerResult, P extends ModulePlugin<P>> extends Aligner<R, P> {

	
	private static final String ALLOW_REVERSE_HSPS = "allowReverseHsps";
	private static final String MINIMUM_SCORE = "minimumScore";
	private static final String MINIMUM_BIT_SCORE = "minimumBitScore";
	private static final String FEATURE_NAME = "featureName";
	
	private BlastRunner blastRunner = new BlastRunner();
	private Optional<Double> minimumBitScore;
	private Optional<Integer> minimumScore;
	private Boolean allowReverseHsps;
	private String featureName;
	
	
	public AbstractBlastAligner() {
		super();
		addSimplePropertyName(ALLOW_REVERSE_HSPS);
		addSimplePropertyName(MINIMUM_BIT_SCORE);
		addSimplePropertyName(MINIMUM_SCORE);
		addSimplePropertyName(FEATURE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minimumBitScore = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MINIMUM_BIT_SCORE, false));
		minimumScore = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MINIMUM_SCORE, false));
		allowReverseHsps = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_REVERSE_HSPS, false)).orElse(false);
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, false);
		
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, "blastRunner");
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
	}

	protected class MyBlastHspFilter implements BlastHspFilter {

		public MyBlastHspFilter() {
		}

		@Override
		public boolean allowBlastHsp(BlastHsp blastHsp) {
			if(minimumBitScore.map(m -> blastHsp.getBitScore() < m).orElse(false)) {
				return false;
			}
			if(minimumScore.map(m -> blastHsp.getScore() < m).orElse(false)) {
				return false;
			}
			if(!allowReverseHsps && 
					( (blastHsp.getQueryTo() < blastHsp.getQueryFrom()) ||
						(blastHsp.getHitTo() < blastHsp.getHitFrom())) ) {
				return false;
			}
			return true;
		}
		
	}

	protected BlastRunner getBlastRunner() {
		return blastRunner;
	}

	protected String getFeatureName() {
		return featureName;
	}
	
	
}
