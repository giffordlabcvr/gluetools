package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class MinorityVariantFilter<S extends AbstractSequenceObject> implements Plugin {

	public static final String MIN_PROPORTION = "minProportion";
	
	private Double minProportion = null;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		minProportion = PluginUtils.configureDoubleProperty(configElem, MIN_PROPORTION, 0.0, true, 1.0, true, false);
	}
	
	protected Double getMinProportion() {
		return minProportion;
	}

	public abstract List<NtMinorityVariant> getMinorityVariants(int ntIndex, S sequenceObject);
	
}
