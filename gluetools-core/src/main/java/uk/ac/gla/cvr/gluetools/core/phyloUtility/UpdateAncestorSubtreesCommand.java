package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;

@CommandClass(
		commandWords={"update-ancestor-subtrees"}, 
		description = "Set a property on all internal nodes which are ancestors of a given set of leaf nodes (including the leaf nodes themselves)", 
		docoptUsages = { },
		docoptOptions = { },
		furtherHelp = "",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class UpdateAncestorSubtreesCommand extends BaseUpdatePhyloTreeCommand {

	@Override
	protected void updatePhyloTree(PhyloTree phyloTree) {
		List<PhyloLeaf> phyloLeaves = findPhyloLeaves(phyloTree);
		Set<PhyloSubtree<?>> phyloSubtrees = new LinkedHashSet<PhyloSubtree<?>>();
		for(PhyloLeaf phyloLeaf: phyloLeaves) {
			PhyloSubtree<?> phyloSubtree = phyloLeaf;
			while(phyloSubtree != null && !phyloSubtrees.contains(phyloSubtree)) {
				phyloSubtrees.add(phyloSubtree);
				PhyloBranch phyloBranch = phyloSubtree.getParentPhyloBranch();
				if(phyloBranch == null) {
					phyloSubtree = null;
				} else {
					phyloSubtree = phyloBranch.getParentPhyloInternal();
				}
			}
		}
		String propertyName = getPropertyName();
		String propertyValue = getPropertyValue();
		for(PhyloSubtree<?> phyloSubtree: phyloSubtrees) {
			phyloSubtree.ensureUserData().put(propertyName, propertyValue);
		}
	}

}
