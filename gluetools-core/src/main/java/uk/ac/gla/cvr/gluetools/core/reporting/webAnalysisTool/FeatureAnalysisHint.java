package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class FeatureAnalysisHint implements Plugin {

	public static final String FEATURE_NAME = "featureName";
	public static final String INCLUDES_SEQUENCE_CONTENT = "includesSequenceContent";
	public static final String DERIVE_SEQUENCE_CONTENT_FROM = "deriveSequenceAnalysisFrom";
	
	private String featureName;
	private Boolean includesSequenceContent;
	private String deriveSequenceAnalysisFrom;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		includesSequenceContent = Optional.ofNullable(
				PluginUtils.configureBooleanProperty(configElem, INCLUDES_SEQUENCE_CONTENT, false)).orElse(false);
		deriveSequenceAnalysisFrom = PluginUtils.configureStringProperty(configElem, DERIVE_SEQUENCE_CONTENT_FROM, false);
	}

	public String getFeatureName() {
		return featureName;
	}

	public Boolean getIncludesSequenceContent() {
		return includesSequenceContent;
	}

	public String getDeriveSequenceAnalysisFrom() {
		return deriveSequenceAnalysisFrom;
	}
	
}
