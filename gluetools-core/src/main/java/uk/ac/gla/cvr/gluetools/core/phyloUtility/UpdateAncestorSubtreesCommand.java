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
		commandWords={"update-ancestor-subtrees"}, 
		description = "Set a property on all internal nodes which are ancestors of a given leaf node (including itself)", 
		docoptUsages = { },
		docoptOptions = { },
		furtherHelp = "",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class UpdateAncestorSubtreesCommand extends BaseUpdatePhyloTreeCommand {

	private static final String SUBTREE_PROPERTY_VALUE = "subtreePropertyValue";
	private static final String SUBTREE_PROPERTY_NAME = "subtreePropertyName";
	private static final String LEAF_NODE_NAME = "leafNodeName";
	private String leafNodeName;
	private String subtreePropertyName;
	private String subtreePropertyValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.leafNodeName = PluginUtils.configureStringProperty(configElem, LEAF_NODE_NAME, true);
		this.subtreePropertyName = PluginUtils.configureStringProperty(configElem, SUBTREE_PROPERTY_NAME, true);
		this.subtreePropertyValue = PluginUtils.configureStringProperty(configElem, SUBTREE_PROPERTY_VALUE, true);
	}

	@Override
	protected void updatePhyloTree(PhyloTree phyloTree) {
		PhyloLeaf phyloLeaf = findPhyloLeaf(phyloTree, leafNodeName);
		PhyloSubtree<?> phyloSubtree = phyloLeaf;
		while(phyloSubtree != null) {
			phyloSubtree.ensureUserData().put(subtreePropertyName, subtreePropertyValue);
			PhyloBranch parentPhyloBranch = phyloSubtree.getParentPhyloBranch();
			if(parentPhyloBranch != null) {
				phyloSubtree = parentPhyloBranch.getParentPhyloInternal();
			} else {
				phyloSubtree = null;
			}
		}
	}

}
