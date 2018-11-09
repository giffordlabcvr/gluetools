package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.assemblyGapSpecifier;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="assemblyGapSpecifier")
public class AssemblyGapSpecifier implements Plugin {

	private String linkageEvidence;
	private int minRunLengthForGap;
	private boolean lengthsKnown;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.linkageEvidence = PluginUtils.configureStringProperty(configElem, "linkageEvidence", true);
		this.minRunLengthForGap = PluginUtils.configureIntProperty(configElem, "minRunLengthForGap", true);
		this.lengthsKnown = PluginUtils.configureBooleanProperty(configElem, "lengthsKnown", true);
	}

	public String getLinkageEvidence() {
		return linkageEvidence;
	}

	public int getMinRunLengthForGap() {
		return minRunLengthForGap;
	}

	public boolean isLengthsKnown() {
		return lengthsKnown;
	}
	
}