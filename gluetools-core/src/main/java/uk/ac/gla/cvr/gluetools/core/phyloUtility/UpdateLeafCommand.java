package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"update-leaf"}, 
		description = "Set a property on a specific leaf node", 
		docoptUsages = { },
		docoptOptions = { },
		furtherHelp = "",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class UpdateLeafCommand extends BaseUpdatePhyloTreeCommand {

	private static final String LEAF_PROPERTY_VALUE = "leafPropertyValue";
	private static final String LEAF_PROPERTY_NAME = "leafPropertyName";
	private static final String LEAF_NODE_NAME = "leafNodeName";
	private String leafNodeName;
	private String leafPropertyName;
	private String leafPropertyValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.leafNodeName = PluginUtils.configureStringProperty(configElem, LEAF_NODE_NAME, true);
		this.leafPropertyName = PluginUtils.configureStringProperty(configElem, LEAF_PROPERTY_NAME, true);
		this.leafPropertyValue = PluginUtils.configureStringProperty(configElem, LEAF_PROPERTY_VALUE, true);
	}

	@Override
	protected void updatePhyloTree(PhyloTree phyloTree) {
		PhyloLeaf phyloLeaf = findPhyloLeaf(phyloTree, leafNodeName);
		phyloLeaf.ensureUserData().put(leafPropertyName, leafPropertyValue);
	}

}
