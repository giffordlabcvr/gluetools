package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Supplier;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.DocumentToPhyloTreeTransformer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"visualise", "tree-document"}, 
		description = "Create visualisation document from a tree document", 
		docoptUsages = { }, 
		metaTags = {CmdMeta.inputIsComplex}	
)
public class VisualiseTreeDocumentCommand extends ModulePluginCommand<VisualiseTreeResult, TreeVisualiser> {

	private static final String MAX_SUBTREE_Y = "maxSubtreeY";
	private static final String MIN_SUBTREE_Y = "minSubtreeY";
	private static final String SUBTREE_Y = "y";
	private static final String SUBTREE_X = "x";
	public final static String TREE_DOCUMENT = "treeDocument";
	public final static String PX_WIDTH = "pxWidth";
	public final static String PX_HEIGHT = "pxHeight";
	public final static String LEAF_NODES = "leafNodes";
	public final static String INTERNAL_NODES = "internalNodes";
	public final static String INTERNAL_VERTICALS = "internalVerticals";
	public final static String BRANCHES = "branches";
	
	private CommandDocument treeDocument;
	private int pxWidth;
	private int pxHeight;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.treeDocument = PluginUtils.configureCommandDocumentProperty(configElem, TREE_DOCUMENT, true);
		this.pxWidth = PluginUtils.configureIntProperty(configElem, PX_WIDTH, true);
		this.pxHeight = PluginUtils.configureIntProperty(configElem, PX_HEIGHT, true);
	}

	@Override
	protected VisualiseTreeResult execute(CommandContext cmdContext, TreeVisualiser treeVisualiser) {
		DocumentToPhyloTreeTransformer docToPhyloTreeTransformer = new DocumentToPhyloTreeTransformer();
		treeDocument.accept(docToPhyloTreeTransformer);
		
		PhyloTree phyloTree = docToPhyloTreeTransformer.getPhyloTree();
		
		CommandDocument visDocument = new CommandDocument("treeVisualisation");
		
		
		BigDecimal maxDepth = getMaxDepth(phyloTree);
		Integer numLeaves = countLeaves(phyloTree);
		
		double treeWidthPct = 100 - (
				treeVisualiser.getRightMarginPct() + 
				treeVisualiser.getRootPct() + 
				treeVisualiser.getLeafInfoPct() + 
				treeVisualiser.getLeftMarginPct()
		); 
		
		double treeWidth = (pxWidth * treeWidthPct) / 100.0;
				
		double treeHeightPct = 100 - (
				treeVisualiser.getTopMarginPct() + 
				treeVisualiser.getBottomMarginPct()
		); 
		
		double treeHeight = (pxHeight * treeHeightPct) / 100.0;
		
		// amount of space in pixels allocated to one leaf.
		double verticalLeafSpace = treeHeight / (double) numLeaves;
		
		// multiply branch lengths by this amount to give branth length in pixels 
		double branchLengthMultiplier = treeWidth / maxDepth.doubleValue();
		
		double rightMarginPx = (pxWidth * treeVisualiser.getRightMarginPct()) / 100.0;
		double rootLengthPx = (pxWidth * treeVisualiser.getRootPct()) / 100.0;
		double topMarginPx = (pxHeight * treeVisualiser.getTopMarginPct()) / 100.0;

		visDocument.setInt(PX_WIDTH, pxWidth);
		visDocument.setInt(PX_HEIGHT, pxHeight);
		
		setSubtreeCoords(phyloTree, rightMarginPx, rootLengthPx, topMarginPx, verticalLeafSpace, branchLengthMultiplier);
		generateObjects(phyloTree, rightMarginPx,  rootLengthPx, topMarginPx, visDocument);
		return new VisualiseTreeResult(visDocument);
	}


	// set x and y positions for all tree nodes, and minSubtreeY / maxSubtreeY for internals.
	private void setSubtreeCoords(PhyloTree phyloTree, 
			double rightMarginPx, double rootLengthPx, double topMarginPx,
			double verticalLeafSpace, double branchLengthMultiplier) {
		
		double xOffset = rightMarginPx + rootLengthPx;
		
		phyloTree.accept(new PhyloTreeVisitor() {
			BigDecimal currentDepth = new BigDecimal(0);
			int leafNumber = 0;
			
			@Override
			public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				currentDepth = currentDepth.add(phyloBranch.getLength());
			}
			@Override
			public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				currentDepth = currentDepth.subtract(phyloBranch.getLength());
			}
			@Override
			public void postVisitInternal(PhyloInternal phyloInternal) {
				double internalX = xOffset + (currentDepth.doubleValue() * branchLengthMultiplier);
				Map<String, Object> phyloInternalData = phyloInternal.ensureUserData();
				phyloInternalData.put(SUBTREE_X, internalX);
				double maxSubtreeY = Double.MIN_VALUE;
				double minSubtreeY = Double.MAX_VALUE;
				for(PhyloBranch phyloBranch: phyloInternal.getBranches()) {
					double subtreeY = (Double) phyloBranch.getSubtree().getUserData().get(SUBTREE_Y);
					minSubtreeY = Math.min(minSubtreeY, subtreeY);
					maxSubtreeY = Math.max(maxSubtreeY, subtreeY);
				}
				phyloInternalData.put(MIN_SUBTREE_Y, minSubtreeY);
				phyloInternalData.put(MAX_SUBTREE_Y, maxSubtreeY);
				phyloInternalData.put(SUBTREE_Y, (maxSubtreeY + minSubtreeY) / 2.0);
				
			}
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				double leafX = xOffset + (currentDepth.doubleValue() * branchLengthMultiplier);
				double leafY = topMarginPx + ( (leafNumber + 0.5) * verticalLeafSpace );
				Map<String, Object> phyloLeafData = phyloLeaf.ensureUserData();
				phyloLeafData.put(SUBTREE_X, leafX);
				phyloLeafData.put(SUBTREE_Y, leafY);
				leafNumber++;
			}
		});
	}

	private void generateObjects(PhyloTree phyloTree, 
			double rightMarginPx, double rootLengthPx, double topMarginPx, 
			CommandDocument visDocument) {

		CommandArray leafNodesArray = visDocument.setArray(LEAF_NODES);
		CommandArray internalNodesArray = visDocument.setArray(INTERNAL_NODES);
		CommandArray internalVerticalsArray = visDocument.setArray(INTERNAL_VERTICALS);
		CommandArray branchesArray = visDocument.setArray(BRANCHES);
		
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				double x = (double) phyloLeaf.getUserData().get(SUBTREE_X);
				double y = (double) phyloLeaf.getUserData().get(SUBTREE_Y);
				CommandObject leafObj = leafNodesArray.addObject();
				leafObj.setDouble("x", x);
				leafObj.setDouble("y", y);
			}
			@Override
			public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				double x1 = (double) phyloBranch.getParentPhyloInternal().getUserData().get(SUBTREE_X);
				double x2 = (double) phyloBranch.getSubtree().getUserData().get(SUBTREE_X);
				double y = (double) phyloBranch.getSubtree().getUserData().get(SUBTREE_Y);
				CommandObject branchObj = branchesArray.addObject();
				branchObj.setDouble("x1", x1);
				branchObj.setDouble("y1", y);
				branchObj.setDouble("x2", x2);
				branchObj.setDouble("y2", y);
			}
			@Override
			public void postVisitInternal(PhyloInternal phyloInternal) {
				double x = (double) phyloInternal.getUserData().get(SUBTREE_X);
				double y = (double) phyloInternal.getUserData().get(SUBTREE_Y);
				CommandObject internalNodeObj = internalNodesArray.addObject();
				internalNodeObj.setDouble("x", x);
				internalNodeObj.setDouble("y", y);

				double y1 = (double) phyloInternal.getUserData().get(MIN_SUBTREE_Y);
				double y2 = (double) phyloInternal.getUserData().get(MAX_SUBTREE_Y);
				CommandObject internalVerticalObj = internalVerticalsArray.addObject();
				internalVerticalObj.setDouble("x1", x);
				internalVerticalObj.setDouble("y1", y1);
				internalVerticalObj.setDouble("x2", x);
				internalVerticalObj.setDouble("y2", y2);
			}
		});
		
		// add root
		PhyloSubtree<?> root = phyloTree.getRoot();
		double x1 = rightMarginPx;
		double x2 = (double) root.getUserData().get(SUBTREE_X);
		double y = (double) root.getUserData().get(SUBTREE_Y);
		CommandObject branchObj = branchesArray.addObject();
		branchObj.setDouble("x1", x1);
		branchObj.setDouble("y1", y);
		branchObj.setDouble("x2", x2);
		branchObj.setDouble("y2", y);

	}

	
	private BigDecimal getMaxDepth(PhyloTree phyloTree) {
		return phyloTree.accept(new PhyloTreeSummariser<BigDecimal>() {
			BigDecimal currentDepth = new BigDecimal(0);
			BigDecimal maxDepth = new BigDecimal(0);
			
			@Override
			public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				currentDepth = currentDepth.add(phyloBranch.getLength());
			}
			@Override
			public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				currentDepth = currentDepth.subtract(phyloBranch.getLength());
			}
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				if(currentDepth.compareTo(maxDepth) > 0) {
					maxDepth = currentDepth;
				}
			}
			@Override
			public BigDecimal get() {
				return maxDepth;
			}
		}).get();
	}
	
	private Integer countLeaves(PhyloTree phyloTree) {
		return phyloTree.accept(new PhyloTreeSummariser<Integer>() {
			Integer numLeaves = 0;
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				numLeaves = numLeaves+1;
			}
			@Override
			public Integer get() {
				return numLeaves;
			}
		}).get();
	}
	
	public interface PhyloTreeSummariser<C> extends PhyloTreeVisitor, Supplier<C> {}

}
