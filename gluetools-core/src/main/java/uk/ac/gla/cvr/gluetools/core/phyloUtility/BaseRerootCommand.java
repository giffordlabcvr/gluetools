package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseRerootCommand extends PhyloUtilityCommand<OkResult> {

	public static final String OUTPUT_FILE = "outputFile";
	public static final String OUTPUT_FORMAT = "outputFormat";

	private String outputFile;
	private PhyloFormat outputFormat;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		this.outputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, OUTPUT_FORMAT, true);
	}
	
	protected void saveRerootedTree(CommandContext cmdContext, PhyloTree rerootedTree) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		consoleCmdContext.saveBytes(outputFile, outputFormat.generate(rerootedTree));
	}

	protected void removeOutgroupSubtree(PhyloTree rerootedTree, PhyloSubtree<?> outgroupSubtree) {
		PhyloObject<?> root = rerootedTree.getRoot();
		if(!(root instanceof PhyloInternal)) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Single leaf node in rerooted tree: cannot remove outgroup.");
		}
		PhyloInternal rootPhyloInternal = (PhyloInternal) root;
		List<PhyloBranch> rootBranches = rootPhyloInternal.getBranches();
		if(rootBranches.size() != 2) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unexpected number of branches ("+rootBranches.size()+") at root: cannot remove outgroup.");
		}
		Integer indexOfOutgroup = null;
		for(int i = 0; i < rootBranches.size(); i++) {
			PhyloBranch branch = rootBranches.get(i);
			PhyloSubtree<?> branchSubtree = branch.getSubtree();
			if(branchSubtree == outgroupSubtree) {
				indexOfOutgroup = branch.getChildBranchIndex();
				break;
			}
		}
		if(indexOfOutgroup == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Outgroup not found at root: cannot remove outgroup.");
		}
		Integer indexOfRemainingTree = 0;
		if(indexOfOutgroup.intValue() == 0) {
			indexOfRemainingTree = 1;
		}
		PhyloBranch remainingTreeBranch = rootBranches.get(indexOfRemainingTree);
		rootPhyloInternal.removeBranch(remainingTreeBranch);
		PhyloSubtree<?> remainingSubtree = remainingTreeBranch.getSubtree();
		rerootedTree.setRoot(remainingSubtree);
	}
	
}
