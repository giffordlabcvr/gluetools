package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.sourceInfoProvider;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class SourceInfoProvider implements Plugin {

	private String sourceModifier;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.sourceModifier = PluginUtils.configureStringProperty(configElem, "sourceModifier", true);
	}

	public String getSourceModifier() {
		return sourceModifier;
	}
	
	
	public void setSourceModifier(String sourceModifier) {
		this.sourceModifier = sourceModifier;
	}

	public abstract String provideSourceInfo(Sequence sequence);
	
}
