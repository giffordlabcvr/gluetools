/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
		registerModulePluginCmdClass(GenerateRaxmlNucleotidePhylogenyCommand.class);
		registerModulePluginCmdClass(GenerateRaxmlAminoAcidPhylogenyCommand.class);
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
