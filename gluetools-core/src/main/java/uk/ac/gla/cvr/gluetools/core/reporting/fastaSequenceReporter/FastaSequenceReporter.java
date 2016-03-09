package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="fastaSequenceReporter")
public class FastaSequenceReporter extends ModulePlugin<FastaSequenceReporter> {

	public static final String ALIGNER_MODULE_NAME = "alignerModuleName";

	private String alignerModuleName;

	public FastaSequenceReporter() {
		super();
		addModulePluginCmdClass(FastaSequenceAminoAcidCommand.class);
		addModulePluginCmdClass(FastaSequenceVariationScanCommand.class);
		addSimplePropertyName(ALIGNER_MODULE_NAME);

	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignerModuleName = PluginUtils.configureStringProperty(configElem, ALIGNER_MODULE_NAME, false);
	}
	
	public String getAlignerModuleName() {
		return alignerModuleName;
	}
	
}
