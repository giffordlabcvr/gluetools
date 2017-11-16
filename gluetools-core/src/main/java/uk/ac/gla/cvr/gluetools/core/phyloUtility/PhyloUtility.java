package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import java.math.BigDecimal;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="phyloUtility",
		description="Provides various operations on phylogenetic trees")
public class PhyloUtility extends ModulePlugin<PhyloUtility> {

	public PhyloUtility() {
		super();
		addModulePluginCmdClass(RerootPhylogenyCommand.class);
		addModulePluginCmdClass(ReformatPhylogenyCommand.class);
	}
	
	public PhyloTree rerootPhylogeny(PhyloBranch rerootBranch, BigDecimal rerootDistance) {
		return PhyloRerooting.rerootPhylogeny(rerootBranch, rerootDistance);
	}
	
}
