package uk.ac.gla.cvr.gluetools.core.reporting.variationAnalyser;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class FeatureAnalysisHint implements Plugin {

	public static final String FEATURE_NAME = "featureName";
	public static final String INCLUDE_TRANSLATION = "includeTranslation";
	
	private String featureName;
	private Boolean includeTranslation;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
		includeTranslation = Optional.ofNullable(
				PluginUtils.configureBooleanProperty(configElem, INCLUDE_TRANSLATION, false)).orElse(false);
	}

	public String getFeatureName() {
		return featureName;
	}

	public Boolean getIncludeTranslation() {
		return includeTranslation;
	}
	
}
