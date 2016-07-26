package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment.navigation;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class NavigationDirection implements Plugin {

	public enum Condition {
		BEFORE_START,
		AFTER_END
	}
	
	public static final String CONDITION = "condition";
	public static final String FEATURE_NAME = "featureName";
	
	private Condition condition;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		this.condition = PluginUtils.configureEnumProperty(Condition.class, configElem, CONDITION, true);
		this.featureName = PluginUtils.configureStringProperty(configElem, FEATURE_NAME, true);
	}

	public Condition getCondition() {
		return condition;
	}

	public String getFeatureName() {
		return featureName;
	}
	
}
