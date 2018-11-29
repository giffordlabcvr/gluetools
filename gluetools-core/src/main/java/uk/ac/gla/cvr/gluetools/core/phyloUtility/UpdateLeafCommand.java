package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;

@CommandClass(
		commandWords={"update-leaves"}, 
		description = "Set a property on a specific set of leaf nodes", 
		docoptUsages = { },
		docoptOptions = { },
		furtherHelp = "",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class UpdateLeafCommand extends BaseUpdatePhyloTreeCommand {

	@Override
	protected void updatePhyloTree(PhyloTree phyloTree) {
		List<PhyloLeaf> phyloLeaves = findPhyloLeaves(phyloTree);
		String propertyName = getPropertyName();
		String propertyValue = getPropertyValue();
		for(PhyloLeaf phyloLeaf: phyloLeaves) {
			phyloLeaf.ensureUserData().put(propertyName, propertyValue);
		}
	}

}
