package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"update-ancestor-branches"}, 
		description = "Set a property on all branches which are ancestors of a given leaf node", 
		docoptUsages = { },
		docoptOptions = { },
		furtherHelp = "",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class UpdateAncestorBranchesCommand extends BaseUpdatePhyloTreeCommand {

	private static final String BRANCH_PROPERTY_VALUE = "branchPropertyValue";
	private static final String BRANCH_PROPERTY_NAME = "branchPropertyName";
	private static final String LEAF_NODE_NAME = "leafNodeName";
	private String leafNodeName;
	private String branchPropertyName;
	private String branchPropertyValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.leafNodeName = PluginUtils.configureStringProperty(configElem, LEAF_NODE_NAME, true);
		this.branchPropertyName = PluginUtils.configureStringProperty(configElem, BRANCH_PROPERTY_NAME, true);
		this.branchPropertyValue = PluginUtils.configureStringProperty(configElem, BRANCH_PROPERTY_VALUE, true);
	}

	@Override
	protected void updatePhyloTree(PhyloTree phyloTree) {
		PhyloLeaf phyloLeaf = findPhyloLeaf(phyloTree, leafNodeName);
		PhyloSubtree<?> phyloSubtree = phyloLeaf;
		while(phyloSubtree != null) {
			PhyloBranch parentPhyloBranch = phyloSubtree.getParentPhyloBranch();
			if(parentPhyloBranch != null) {
				parentPhyloBranch.ensureUserData().put(branchPropertyName, branchPropertyValue);
				phyloSubtree = parentPhyloBranch.getParentPhyloInternal();
			} else {
				phyloSubtree = null;
			}
		}
	}

}
