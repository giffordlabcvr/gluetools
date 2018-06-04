package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@PluginClass(elemName="alignmentProvider")
public class AlignmentFeatureProvider extends FeatureProvider {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}

	@Override
	public String provideFeature(Sequence sequence) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
