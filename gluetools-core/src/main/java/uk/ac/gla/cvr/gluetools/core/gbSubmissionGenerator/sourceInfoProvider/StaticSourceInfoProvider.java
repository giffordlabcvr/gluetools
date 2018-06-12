package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="staticSourceInfoProvider")
public class StaticSourceInfoProvider extends SourceInfoProvider {

	private String value;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.value = PluginUtils.configureStringProperty(configElem, "value", true);
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String provideSourceInfo(Sequence sequence) {
		return value;
	}
	
}
