package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
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
import uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator.MemberAnnotationGenerator;

@CommandClass(
		commandWords={"visualise", "tree-document"}, 
		description = "Create visualisation document from a tree document", 
		docoptUsages = { }, 
		metaTags = {CmdMeta.inputIsComplex}	
)
public class VisualiseTreeDocumentCommand extends ModulePluginCommand<VisualiseTreeResult, TreeVisualiser> {

	public final static String TREE_DOCUMENT = "treeDocument";
	public final static String PX_WIDTH = "pxWidth";
	public final static String PX_HEIGHT = "pxHeight";
	public final static String LEAF_TEXT_WIDTH_PX = "leafTextWidthPx";
	public final static String LEAF_TEXT_HEIGHT_PX = "leafTextHeightPx";
	public final static String LEAF_TEXT_GAP_PX = "leafTextGapPx";
	public final static String LEAF_TEXT_ANNOTATION_NAME = "leafTextAnnotationName";

	private CommandDocument treeDocument;
	private int pxWidth;
	private int pxHeight;
	private String leafTextAnnotationName;

	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.treeDocument = PluginUtils.configureCommandDocumentProperty(configElem, TREE_DOCUMENT, true);
		this.pxWidth = PluginUtils.configureIntProperty(configElem, PX_WIDTH, true);
		this.pxHeight = PluginUtils.configureIntProperty(configElem, PX_HEIGHT, true);
		this.leafTextAnnotationName = PluginUtils.configureStringProperty(configElem, LEAF_TEXT_ANNOTATION_NAME, true);
	}

	// phyloTree user data constants
	private static final String MAX_SUBTREE_Y = "maxSubtreeY";
	private static final String MIN_SUBTREE_Y = "minSubtreeY";
	private static final String SUBTREE_Y = "y";
	private static final String SUBTREE_X = "x";

	// visualisation document constants
	private final static String LEAF_NODES = "leafNodes";
	private final static String INTERNAL_NODES = "internalNodes";
	private final static String BRANCHES = "branches";
	private final static String ROOT = "root";
	

	
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
				treeVisualiser.getLeafTextGapPct() + 
				treeVisualiser.getLeafTextPct() + 
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
		double leafTextGapPx = (pxWidth * treeVisualiser.getLeafTextGapPct()) / 100.0;
		double leafTextWidthPx = (pxWidth * treeVisualiser.getLeafTextPct()) / 100.0;
		double rootLengthPx = (pxWidth * treeVisualiser.getRootPct()) / 100.0;
		double topMarginPx = (pxHeight * treeVisualiser.getTopMarginPct()) / 100.0;

		visDocument.setInt(PX_WIDTH, pxWidth);
		visDocument.setInt(PX_HEIGHT, pxHeight);
		visDocument.setDouble(LEAF_TEXT_WIDTH_PX, leafTextWidthPx);
		visDocument.setDouble(LEAF_TEXT_HEIGHT_PX, verticalLeafSpace);
		visDocument.setDouble(LEAF_TEXT_GAP_PX, leafTextGapPx);
		
		setSubtreeCoords(phyloTree, rightMarginPx, rootLengthPx, topMarginPx, verticalLeafSpace, branchLengthMultiplier);
		generateObjects(cmdContext, treeVisualiser, phyloTree, rightMarginPx,  rootLengthPx, topMarginPx, visDocument);
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

	private void generateObjects(CommandContext cmdContext, TreeVisualiser treeVisualiser, PhyloTree phyloTree, 
			double rightMarginPx, double rootLengthPx, double topMarginPx, 
			CommandDocument visDocument) {

		CommandArray leafNodesArray = visDocument.setArray(LEAF_NODES);
		CommandArray internalNodesArray = visDocument.setArray(INTERNAL_NODES);
		CommandArray branchesArray = visDocument.setArray(BRANCHES);
		CommandObject rootObj = visDocument.setObject(ROOT);
		
		List<MemberAnnotationGenerator> memberAnnotationGenerators = treeVisualiser.getMemberAnnotationGenerators();
		
		MemberAnnotationGenerator leafTextAnnotationGenerator = null;
		for(MemberAnnotationGenerator memberAnnotationGenerator: memberAnnotationGenerators) {
			if(memberAnnotationGenerator.getAnnotationName().equals(leafTextAnnotationName)) {
				leafTextAnnotationGenerator = memberAnnotationGenerator;
				break;
			}
		}
		if(leafTextAnnotationGenerator == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "TreeVisualiser module has no generator for annotations named '"+leafTextAnnotationName+"'");
		}
		final MemberAnnotationGenerator leafTextAnnotationGeneratorFinal = leafTextAnnotationGenerator;
		
		// We use the code below to predict font widths from text. 
		// The following system propety is set in the GLUE engine to prevent a desktop icon popping up
		// when using AWT classes.
		// System.setProperty("java.awt.headless", "true"); 
		AffineTransform affineTransform = new AffineTransform();     
		FontRenderContext fontRenderContext = new FontRenderContext(affineTransform, true, true);     
		Font font = new Font(treeVisualiser.getLeafTextFont(), 0, 22); 
		
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				Map<String, Object> leafUserData = phyloLeaf.getUserData();
				double x = (double) leafUserData.get(SUBTREE_X);
				double y = (double) leafUserData.get(SUBTREE_Y);
				CommandObject leafObj = leafNodesArray.addObject();
				leafObj.setDouble("x", x);
				leafObj.setDouble("y", y);
				
				CommandObject leafPropertiesObj = leafObj.setObject("properties");
				String phyloLeafName = phyloLeaf.getName();
				leafPropertiesObj.set("name", phyloLeafName);
				boolean storedAlmtMember = true;
				boolean highlighted = false;
				if(leafUserData != null) {
					Object nonMemberValue = leafUserData.get("treevisualiser-nonmember");
					if(nonMemberValue != null && nonMemberValue.equals("true")) {
						storedAlmtMember = false;
					}
					Object highlightedValue = leafUserData.get("treevisualiser-highlighted");
					if(highlightedValue != null && highlightedValue.equals("true")) {
						highlighted = true;
					}
				} 
				String leafText;
				if(storedAlmtMember) {
					Map<String, String> memberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, phyloLeafName);
					AlignmentMember member = GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap);
					leafText = leafTextAnnotationGeneratorFinal.renderAnnotation(member);
				} else {
					leafText = phyloLeafName;
				}
				leafPropertiesObj.set("leafText", leafText);

				double leafTextWidth100pt = font.getStringBounds(leafText, fontRenderContext).getWidth();  
				leafPropertiesObj.set("leafTextWidth100pt", leafTextWidth100pt);
				
				if(highlighted) {
					leafObj.setBoolean("highlighted", true);
				}

			}
			@Override
			public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				Map<String, Object> parentUserData = phyloBranch.getParentPhyloInternal().getUserData();
				double parentX = (double) parentUserData.get(SUBTREE_X);
				double parentY = (double) parentUserData.get(SUBTREE_Y);
				Map<String, Object> childUserData = phyloBranch.getSubtree().getUserData();
				double childX = (double) childUserData.get(SUBTREE_X);
				double childY = (double) childUserData.get(SUBTREE_Y);
				Map<String, Object> branchUserData = phyloBranch.getUserData();
				boolean highlighted = false;
				if(branchUserData != null) {
					Object highlightedValue = branchUserData.get("treevisualiser-highlighted");
					if(highlightedValue != null && highlightedValue.equals("true")) {
						highlighted = true;
					}
				} 
				
				CommandObject branchObj = branchesArray.addObject();
				branchObj.setDouble("parentX", parentX);
				branchObj.setDouble("parentY", parentY);
				branchObj.setDouble("cornerX", parentX);
				branchObj.setDouble("cornerY", childY);
				branchObj.setDouble("childX", childX);
				branchObj.setDouble("childY", childY);
				if(highlighted) {
					branchObj.setBoolean("highlighted", true);
				}
			}
			@Override
			public void postVisitInternal(PhyloInternal phyloInternal) {
				double x = (double) phyloInternal.getUserData().get(SUBTREE_X);
				double y = (double) phyloInternal.getUserData().get(SUBTREE_Y);
				CommandObject internalNodeObj = internalNodesArray.addObject();
				internalNodeObj.setDouble("x", x);
				internalNodeObj.setDouble("y", y);
			}
		});
		
		// add root
		PhyloSubtree<?> root = phyloTree.getRoot();
		double x1 = rightMarginPx;
		double x2 = (double) root.getUserData().get(SUBTREE_X);
		double y = (double) root.getUserData().get(SUBTREE_Y);
		rootObj.setDouble("x1", x1);
		rootObj.setDouble("y1", y);
		rootObj.setDouble("x2", x2);
		rootObj.setDouble("y2", y);

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
