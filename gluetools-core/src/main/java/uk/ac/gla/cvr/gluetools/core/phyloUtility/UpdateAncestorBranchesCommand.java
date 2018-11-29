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
		commandWords={"update-ancestor-branches"}, 
		description = "Set a property on all branches which are ancestors of a set of leaf nodes", 
		docoptUsages = { },
		docoptOptions = { },
		furtherHelp = "",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class UpdateAncestorBranchesCommand extends BaseUpdatePhyloTreeCommand {


	@Override
	protected void updatePhyloTree(PhyloTree phyloTree) {
		List<PhyloLeaf> phyloLeaves = findPhyloLeaves(phyloTree);
		Set<PhyloBranch> phyloBranches = new LinkedHashSet<PhyloBranch>();
		for(PhyloLeaf phyloLeaf: phyloLeaves) {
			PhyloBranch phyloBranch = phyloLeaf.getParentPhyloBranch();
			while(phyloBranch != null && !phyloBranches.contains(phyloBranch)) {
				phyloBranches.add(phyloBranch);
				PhyloSubtree<?> phyloSubtree = phyloBranch.getParentPhyloInternal();
				phyloBranch = phyloSubtree.getParentPhyloBranch();
			}
		}
		String propertyName = getPropertyName();
		String propertyValue = getPropertyValue();
		for(PhyloBranch phyloBranch: phyloBranches) {
			phyloBranch.ensureUserData().put(propertyName, propertyValue);
		}
	}

		
		
}
