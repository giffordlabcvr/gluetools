package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BaseUpdatePhyloTreeCommand extends BasePhyloTreeCommand<PhyloTreeResult> {

	private static final String PROPERTY_VALUE = "propertyValue";
	private static final String PROPERTY_NAME = "propertyName";
	private static final String LEAF_NODE_NAMES = "leafNodeNames";
	private String propertyName;
	private String propertyValue;
	private List<String> leafNodeNames;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.propertyName = PluginUtils.configureStringProperty(configElem, PROPERTY_NAME, true);
		this.propertyValue = PluginUtils.configureStringProperty(configElem, PROPERTY_VALUE, true);
		this.leafNodeNames = PluginUtils.configureStringsProperty(configElem, LEAF_NODE_NAMES, 1, null);
	}

	
	@Override
	protected final PhyloTreeResult execute(CommandContext cmdContext, PhyloUtility phyloUtility, PhyloTree phyloTree) {
		updatePhyloTree(phyloTree);
		return new PhyloTreeResult(phyloTree);
	}

	protected abstract void updatePhyloTree(PhyloTree phyloTree);
	
	protected List<PhyloLeaf> findPhyloLeaves(PhyloTree phyloTree) {
		Set<String> remainingLeafNodeNames = new LinkedHashSet<String>(leafNodeNames);
		List<PhyloLeaf> phyloLeaves = new ArrayList<PhyloLeaf>();
		
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafName = phyloLeaf.getName();
				boolean removed = remainingLeafNodeNames.remove(leafName);
				if(removed) {
					phyloLeaves.add(phyloLeaf);
				}
			}
		});
		if(!remainingLeafNodeNames.isEmpty()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Phylo leaves with names "+
					new ArrayList<String>(remainingLeafNodeNames).toString()+" not found");
		}
		return phyloLeaves;
	}

	protected String getPropertyName() {
		return propertyName;
	}

	protected String getPropertyValue() {
		return propertyValue;
	}
	
	
	
}
