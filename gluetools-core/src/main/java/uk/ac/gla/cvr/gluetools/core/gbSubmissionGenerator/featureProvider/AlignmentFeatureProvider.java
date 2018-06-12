package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class AlignmentFeatureProvider extends FeatureProvider {

	public static final String GLUE_FEATURE_NAME = "glueFeatureName";
	public static final String MIN_COVERAGE_PCT = "minCoveragePct";
	public static final String FEATURE_KEY = "featureKey";
	public static final String QUALIFIER = "qualifier";
	public static final String SPAN_INSERTIONS = "spanInsertions";
	
	private String glueFeatureName;
	// if the alignment member does not cover at least this percentage of the 
	// named feature on the reference, no GenBank feature is generated.
	private Double minCoveragePct;

	// default true: if true, insertions in the alignment member relative to the reference will be spanned.
	private Boolean spanInsertions;

	
	private String featureKey;
	private List<QualifierKeyValueTemplate> qualifierKeyValueTemplates;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.glueFeatureName = PluginUtils.configureStringProperty(configElem, GLUE_FEATURE_NAME, true);
		this.minCoveragePct = Optional.ofNullable(PluginUtils
				.configureDoubleProperty(configElem, MIN_COVERAGE_PCT, 0.0, false, 100.0, true, false))
				.orElse(10.0);
		this.featureKey = PluginUtils.configureStringProperty(configElem, FEATURE_KEY, true);
		this.qualifierKeyValueTemplates = PluginFactory.createPlugins(pluginConfigContext, QualifierKeyValueTemplate.class, 
				PluginUtils.findConfigElements(configElem, QUALIFIER));
		this.spanInsertions = Optional.ofNullable(PluginUtils
				.configureBooleanProperty(configElem, SPAN_INSERTIONS, false)).orElse(true);

	}

	protected Double getMinCoveragePct() {
		return minCoveragePct;
	}

	protected String getGlueFeatureName() {
		return glueFeatureName;
	}

	protected String getFeatureKey() {
		return featureKey;
	}

	protected Boolean getSpanInsertions() {
		return spanInsertions;
	}

	protected Map<String, String> generateQualifierKeyValuesFromFeatureLocation(
			CommandContext cmdContext, FeatureLocation featureLocation) {
		Map<String, String> qualifierKeyValues = new LinkedHashMap<String, String>();
		qualifierKeyValueTemplates.forEach(qkvt -> 
			qualifierKeyValues.put(qkvt.getKey(), qkvt.generateValueFromFeatureLocation(cmdContext, featureLocation)));
		return qualifierKeyValues;
	}
	
}
