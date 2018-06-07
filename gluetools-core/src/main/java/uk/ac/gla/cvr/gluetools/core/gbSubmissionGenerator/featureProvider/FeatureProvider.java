package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

public abstract class FeatureProvider implements Plugin {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
	}

	
	public abstract String provideFeature(Sequence sequence);
	
}
