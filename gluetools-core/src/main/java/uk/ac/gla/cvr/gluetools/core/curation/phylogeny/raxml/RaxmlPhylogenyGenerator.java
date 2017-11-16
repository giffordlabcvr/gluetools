package uk.ac.gla.cvr.gluetools.core.curation.phylogeny.raxml;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.curation.phylogeny.PhylogenyGenerator;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.raxml.phylogeny.RaxmlPhylogenyRunner;

@PluginClass(elemName="raxmlPhylogenyGenerator",
		description="Uses RAxML to generate a phylogenetic tree from a project Alignment")
public class RaxmlPhylogenyGenerator extends PhylogenyGenerator<RaxmlPhylogenyGenerator>{

	private static final String RAXML_PHYLOGENY_RUNNER = "raxmlPhylogenyRunner";
	private RaxmlPhylogenyRunner raxmlPhylogenyRunner = new RaxmlPhylogenyRunner();
	
	public RaxmlPhylogenyGenerator() {
		super();
		addModulePluginCmdClass(GenerateRaxmlNucleotidePhylogenyCommand.class);
		raxmlPhylogenyRunner.configurePropertyGroup(getRootPropertyGroup().addChild(RAXML_PHYLOGENY_RUNNER));
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		Element raxmlPhylogenyRunnerElem = PluginUtils.findConfigElement(configElem, RAXML_PHYLOGENY_RUNNER);
		if(raxmlPhylogenyRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, raxmlPhylogenyRunnerElem, raxmlPhylogenyRunner);
		}
	}

	public RaxmlPhylogenyRunner getRaxmlPhylogenyRunner() {
		return raxmlPhylogenyRunner;
	}
}
