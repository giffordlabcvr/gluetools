package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import java.util.function.Predicate;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeafFinder;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;

public abstract class BaseUpdatePhyloTreeCommand extends BasePhyloTreeCommand<PhyloTreeResult> {

	@Override
	protected final PhyloTreeResult execute(CommandContext cmdContext, PhyloUtility phyloUtility, PhyloTree phyloTree) {
		updatePhyloTree(phyloTree);
		return new PhyloTreeResult(phyloTree);
	}

	protected abstract void updatePhyloTree(PhyloTree phyloTree);
	
	protected PhyloLeaf findPhyloLeaf(PhyloTree phyloTree, String leafName) {
		PhyloLeafFinder phyloLeafFinder = new PhyloLeafFinder(new Predicate<PhyloLeaf>() {
			@Override
			public boolean test(PhyloLeaf phyloLeaf) {
				return phyloLeaf.getName().equals(leafName);
			}
		});
		phyloTree.accept(phyloLeafFinder);
		PhyloLeaf phyloLeaf = phyloLeafFinder.getPhyloLeaf();
		if(phyloLeaf == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Phylo leaf '"+leafName+"' not found");
		}
		return phyloLeaf;
	}
}
