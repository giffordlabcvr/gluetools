package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class CladeCategory implements Plugin {

	public static final String NAME = "name";
	public static final String DISPLAY_NAME = "displayName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String DISTANCE_SCALING_EXPONENT = "distanceScalingExponent";
	public static final String DISTANCE_CUTOFF = "distanceCutoff";

	private String name;
	private String displayName;
	private Expression whereClause;
	private Double distanceScalingExponent;
	private Double distanceCutoff;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.name = PluginUtils.configureStringProperty(configElem, NAME, true);
		this.displayName = PluginUtils.configureStringProperty(configElem, DISPLAY_NAME, true);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, true);
		this.distanceCutoff = PluginUtils.configureDoubleProperty(configElem, DISTANCE_CUTOFF, true);
		this.distanceScalingExponent = PluginUtils.configureDoubleProperty(configElem, DISTANCE_SCALING_EXPONENT, true);
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Expression getWhereClause() {
		return whereClause;
	}

	public Double getDistanceScalingExponent() {
		return distanceScalingExponent;
	}

	public Double getDistanceCutoff() {
		return distanceCutoff;
	}
}
