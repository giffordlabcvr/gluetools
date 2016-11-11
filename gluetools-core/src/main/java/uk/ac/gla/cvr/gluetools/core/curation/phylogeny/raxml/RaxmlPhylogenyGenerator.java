package uk.ac.gla.cvr.gluetools.core.curation.phylogeny.raxml;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.curation.phylogeny.PhylogenyGenerator;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.raxml.phylogeny.RaxmlPhylogenyRunner;

@PluginClass(elemName="raxmlPhylogenyGenerator")
public class RaxmlPhylogenyGenerator extends PhylogenyGenerator<RaxmlPhylogenyGenerator>{

	private RaxmlPhylogenyRunner raxmlPhylogenyRunner = new RaxmlPhylogenyRunner();
	
	public RaxmlPhylogenyGenerator() {
		super();
		addModulePluginCmdClass(GenerateRaxmlNucleotidePhylogenyCommand.class);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element raxmlPhylogenyRunnerElem = PluginUtils.findConfigElement(configElem, "raxmlPhylogenyRunner");
		if(raxmlPhylogenyRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, raxmlPhylogenyRunnerElem, raxmlPhylogenyRunner);
		}
	}

	public RaxmlPhylogenyRunner getRaxmlPhylogenyRunner() {
		return raxmlPhylogenyRunner;
	}
}
