package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class VariationScanHint implements Plugin {

	public static final String REFERENCE_SEQUENCE = "referenceSequence";
	public static final String MULTI_REFERENCE = "multiReference";
	public static final String DESCENDENT_FEATURES = "descendentFeatures";
	
	private String referenceName;
	private Boolean multiReference;
	private Boolean descendentFeatures;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		referenceName = PluginUtils.configureStringProperty(configElem, REFERENCE_SEQUENCE, true);
		multiReference = Optional.ofNullable(
				PluginUtils.configureBooleanProperty(configElem, MULTI_REFERENCE, false)).orElse(false);
		descendentFeatures = Optional.ofNullable(
				PluginUtils.configureBooleanProperty(configElem, DESCENDENT_FEATURES, false)).orElse(false);
	}

	public String getReferenceName() {
		return referenceName;
	}

	public Boolean getMultiReference() {
		return multiReference;
	}

	public Boolean getDescendentFeatures() {
		return descendentFeatures;
	}

}
