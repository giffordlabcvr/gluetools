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

	public static final String TREE_DOCUMENT = "treeDocument";
	public static final String PX_WIDTH = "pxWidth";
	public static final String PX_HEIGHT = "pxHeight";
	public static final String LEAF_TEXT_ANNOTATION_NAME = "leafTextAnnotationName";

	private static final double COLLAPSED_SUBTREE_VERTICAL_PROPORTION = 0.8;
	private static final double COLLAPSED_LEAF_WIDTH_PROPORTION = 0.5;

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

	// phyloTree user data and visualisation document constants
	private static final String SUBTREE_Y = "y";
	private static final String SUBTREE_X = "x";
	private static final String COLLAPSED_WIDTH_PX = "collapsedWidthPx";
	private static final String COLLAPSED_HEIGHT_PX = "collapsedHeightPx";
	private static final String LEAF_NODES = "leafNodes";
	private static final String INTERNAL_NODES = "internalNodes";
	private static final String COLLAPSED_SUBTREES = "collapsedSubtrees";
	private static final String BRANCHES = "branches";
	private static final String ROOT = "root";
	private static final String LEAF_BRANCH_DEPTH = "leafBranchDepth";
	private static final String MIN_BRANCH_DEPTH = "minBranchDepth";
	private static final String MAX_BRANCH_DEPTH = "maxBranchDepth";
	private static final String LEAF_TEXT = "leafText";
	private static final String LEAF_TEXT_WIDTH_PX = "leafTextWidthPx";
	private static final String COLLAPSED_TEXT = "collapsedText";
	private static final String COLLAPSED_TEXT_WIDTH_PX = "collapsedTextWidthPx";
	private static final String VERTICAL_LEAF_SPACE_PX = "verticalLeafSpacePx";
	private static final String LEAF_TEXT_GAP_PX = "leafTextGapPx";
	private static final String LEAF_TEXT_HEIGHT_PX = "leafTextHeightPx";
	private static final String LEAF_TEXT_HEIGHT_PROPORTION = "leafTextHeightProportion";
	private static final String STARTING_LEAF_UNIT = "startingLeafUnit";
	private static final String LEAF_UNITS = "leafUnits";
	
	
	@Override
	protected VisualiseTreeResult execute(CommandContext cmdContext, TreeVisualiser treeVisualiser) {
		DocumentToPhyloTreeTransformer docToPhyloTreeTransformer = new DocumentToPhyloTreeTransformer();
		treeDocument.accept(docToPhyloTreeTransformer);
		
		PhyloTree phyloTree = docToPhyloTreeTransformer.getPhyloTree();
		
		// calculate tree width in pixels including tree, leaf gap and leaf text but not including root.
		double treeWidthPct = 100 - (
				treeVisualiser.getRightMarginPct() + 
				treeVisualiser.getRootPct() + 
				treeVisualiser.getLeftMarginPct()
		); 
		double treeWidthPx = (pxWidth * treeWidthPct) / 100.0;

		// vertical space allocated to tree
		double treeHeightPct = 100 - (
				treeVisualiser.getTopMarginPct() + 
				treeVisualiser.getBottomMarginPct()
		); 
		double availableTreeVerticalSpacePx = (pxHeight * treeHeightPct) / 100.0;
		
		Integer numLeafUnits = allocateLeafUnits(phyloTree, 
				treeVisualiser.getMinCollapsedSubtreeLeafUnits(), treeVisualiser.getMaxCollapsedSubtreeLeafUnits());
		
		// amount of space in pixels allocated to one leaf unit.
		double verticalLeafUnitSpacePx = Math.min(availableTreeVerticalSpacePx / (double) numLeafUnits, 
				treeVisualiser.getMaxVerticalLeafUnitSpacePx()); // apply maximum to avoid huge text which mucks up tree!

		double leafTextHeightPx = verticalLeafUnitSpacePx * treeVisualiser.getLeafTextHeightProportion(); 

		setBranchAndTextProperties(cmdContext, treeVisualiser, phyloTree, leafTextHeightPx);
		
		// gap in pixels between leaf node tip and leaf text
		double leafTextGapPx = (pxWidth * treeVisualiser.getLeafTextGapPct()) / 100.0;
		
		// Calculate the amount to multiply branch lengths by to give branth length in pixels 
		double branchLengthMultiplier = getBranchLengthMultiplier(phyloTree, treeWidthPx, leafTextGapPx);
		
		double rightMarginPx = (pxWidth * treeVisualiser.getRightMarginPct()) / 100.0;
		double rootLengthPx = (pxWidth * treeVisualiser.getRootPct()) / 100.0;
		double topMarginPx = (pxHeight * treeVisualiser.getTopMarginPct()) / 100.0;
		double bottomMarginPx = (pxHeight * treeVisualiser.getBottomMarginPct()) / 100.0;

		int finalHeightPx = (int) Math.ceil((topMarginPx + (numLeafUnits * verticalLeafUnitSpacePx) + bottomMarginPx));
		
		CommandDocument visDocument = new CommandDocument("treeVisualisation");
		visDocument.setInt(PX_WIDTH, pxWidth);
		visDocument.setInt(PX_HEIGHT, finalHeightPx);
		visDocument.setDouble(VERTICAL_LEAF_SPACE_PX, verticalLeafUnitSpacePx);
		visDocument.setDouble(LEAF_TEXT_HEIGHT_PX, leafTextHeightPx);
		visDocument.setDouble(LEAF_TEXT_GAP_PX, leafTextGapPx);
		visDocument.setDouble(LEAF_TEXT_HEIGHT_PROPORTION, treeVisualiser.getLeafTextHeightProportion());
		
		setSubtreeCoords(phyloTree, rightMarginPx, rootLengthPx, topMarginPx, verticalLeafUnitSpacePx, branchLengthMultiplier);
		generateObjects(cmdContext, treeVisualiser, phyloTree, rightMarginPx,  rootLengthPx, topMarginPx, visDocument);
		return new VisualiseTreeResult(visDocument);
	}


	// on non-collapsed leaf nodes outside collapsed nodes, set these properties: 
	//     -- leafBranchDepth (branch length units)
	//     -- leafText
	//     -- leafTextWidthPx (based on font, leaf text height proportion, vertical leaf space and leaf text)
	
	// on collapsed internal / leafNodes: 
	//     -- minBranchDepth, maxBranchDepth (branch length units)
	//     -- collapsedText
	//     -- collapsedTextWidthPx (based on font, leaf text height proportion, vertical leaf space and leaf text)
	

	
	private void setBranchAndTextProperties(CommandContext cmdContext, TreeVisualiser treeVisualiser, PhyloTree phyloTree, double leafTextHeightPx) {
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
		// fonts must use integer sizes
		// we will use a 100pt font to figure out the width, then re-scale it as necessary.
		Font font = new Font(treeVisualiser.getLeafTextFont(), 0, 100); 
		
		phyloTree.accept(new PhyloTreeVisitor() {
			BigDecimal currentDepth = new BigDecimal(0);
			
			PhyloInternal currentCollapsed = null;
			
			@Override
			public void preVisitInternal(PhyloInternal phyloInternal) {
				Map<String, Object> internalUserData = phyloInternal.getUserData();
				if(currentCollapsed == null) {
					if(isCollapsed(phyloInternal)) {
						currentCollapsed = phyloInternal;
						
						internalUserData.put(MIN_BRANCH_DEPTH, currentDepth.doubleValue());

						String collapsedLabel = (String) internalUserData.get("treevisualiser-collapsedLabel");
						if(collapsedLabel == null) {
							collapsedLabel = "?";
						}
						internalUserData.put(COLLAPSED_TEXT, collapsedLabel);
						double collapsedTextWidthPx = getTextWidthPx(fontRenderContext, font, leafTextHeightPx, collapsedLabel);  
						internalUserData.put(COLLAPSED_TEXT_WIDTH_PX, collapsedTextWidthPx);
					}
				}
			}
			@Override
			public void postVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == phyloInternal) {
					currentCollapsed = null;
				}
			}

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
				if(currentCollapsed == null) {
					Map<String, Object> leafUserData = phyloLeaf.getUserData();
					if(isCollapsed(phyloLeaf)) {
						
						leafUserData.put(MIN_BRANCH_DEPTH, currentDepth.doubleValue() - 
								( phyloLeaf.getParentPhyloBranch().getLength().doubleValue()) * COLLAPSED_LEAF_WIDTH_PROPORTION );
						leafUserData.put(MAX_BRANCH_DEPTH, currentDepth.doubleValue());
						
						String collapsedLabel = (String) leafUserData.get("treevisualiser-collapsedLabel");
						if(collapsedLabel == null) {
							collapsedLabel = "?";
						}
						leafUserData.put(COLLAPSED_TEXT, collapsedLabel);
						double collapsedTextWidthPx = getTextWidthPx(fontRenderContext, font, leafTextHeightPx, collapsedLabel);  
						leafUserData.put(COLLAPSED_TEXT_WIDTH_PX, collapsedTextWidthPx);
					} else {
						leafUserData.put(LEAF_BRANCH_DEPTH, currentDepth.doubleValue());
						String phyloLeafName = phyloLeaf.getName();
						boolean storedAlmtMember = true;
						if(leafUserData != null) {
							Object nonMemberValue = leafUserData.get("treevisualiser-nonmember");
							if(nonMemberValue != null && nonMemberValue.equals("true")) {
								storedAlmtMember = false;
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
						leafUserData.put(LEAF_TEXT, leafText);

						double leafTextWidthPx = getTextWidthPx(fontRenderContext, font, leafTextHeightPx, leafText);  
						leafUserData.put(LEAF_TEXT_WIDTH_PX, leafTextWidthPx);
					}
				} else {
					// update the max branch depth of the current collapsed internal node.
					Map<String, Object> collapsedInternalUserData = currentCollapsed.getUserData();
					Double currentMaxBranchDepth = (Double) collapsedInternalUserData.get(MAX_BRANCH_DEPTH);
					if(currentMaxBranchDepth == null) {
						currentMaxBranchDepth = Double.MIN_VALUE;
					}
					currentMaxBranchDepth = Math.max(currentMaxBranchDepth, currentDepth.doubleValue());
					collapsedInternalUserData.put(MAX_BRANCH_DEPTH, currentMaxBranchDepth);
				}
			}
			
			private double getTextWidthPx(FontRenderContext fontRenderContext, Font font, double textHeightPx,
					String text) {
				return (font.getStringBounds(text, fontRenderContext).getWidth() / 100) * textHeightPx;
			}
		});
	}


	// set x and y positions for all internal and leaf nodes outside of collapsed nodes.
	// for collapsed nodes it sets x and y positions for the root of the collapsed subtree, 
	// and collapsedWidthPx / collapsedHeightPx values for the dimensions of the subtree.
	private void setSubtreeCoords(PhyloTree phyloTree, 
			double rightMarginPx, double rootLengthPx, double topMarginPx,
			double verticalLeafSpace, double branchLengthMultiplier) {
		
		double xOffset = rightMarginPx + rootLengthPx;
		
		phyloTree.accept(new PhyloTreeVisitor() {
			BigDecimal currentDepth = new BigDecimal(0);
			PhyloInternal currentCollapsed = null;

			@Override
			public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				currentDepth = currentDepth.add(phyloBranch.getLength());
			}
			@Override
			public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				currentDepth = currentDepth.subtract(phyloBranch.getLength());
			}

			@Override
			public void preVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == null) {
					if(isCollapsed(phyloInternal)) {
						currentCollapsed = phyloInternal;

						Map<String, Object> phyloInternalData = phyloInternal.ensureUserData();
						int startingLeafUnit = (Integer) phyloInternalData.get(STARTING_LEAF_UNIT);
						int leafUnits = (Integer) phyloInternalData.get(LEAF_UNITS);
						double minBranchDepth = (Double) phyloInternalData.get(MIN_BRANCH_DEPTH);
						double maxBranchDepth = (Double) phyloInternalData.get(MAX_BRANCH_DEPTH);
						double collapsedInternalX = xOffset + (minBranchDepth * branchLengthMultiplier);
						double collapsedInternalY = topMarginPx + ( (startingLeafUnit + (leafUnits * 0.5)) * verticalLeafSpace );
						phyloInternalData.put(SUBTREE_X, collapsedInternalX);
						phyloInternalData.put(SUBTREE_Y, collapsedInternalY);
						double collapsedWidthPx = ((maxBranchDepth - minBranchDepth) * branchLengthMultiplier);
						double collapsedHeightPx = leafUnits * verticalLeafSpace;
						phyloInternalData.put(COLLAPSED_WIDTH_PX, collapsedWidthPx);
						phyloInternalData.put(COLLAPSED_HEIGHT_PX, collapsedHeightPx);
					}
				}
			}
			
			@Override
			public void postVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == null) {
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
					phyloInternalData.put(SUBTREE_Y, (maxSubtreeY + minSubtreeY) / 2.0);				
				} else if(currentCollapsed == phyloInternal) {
					currentCollapsed = null;
				}
				
			}
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				if(currentCollapsed == null) {
					Map<String, Object> phyloLeafData = phyloLeaf.ensureUserData();
					int startingLeafUnit = (Integer) phyloLeafData.get(STARTING_LEAF_UNIT);
					if(isCollapsed(phyloLeaf)) {
						int leafUnits = (Integer) phyloLeafData.get(LEAF_UNITS);
						double minBranchDepth = (Double) phyloLeafData.get(MIN_BRANCH_DEPTH);
						double maxBranchDepth = (Double) phyloLeafData.get(MAX_BRANCH_DEPTH);
						double leafX = xOffset + (minBranchDepth * branchLengthMultiplier);
						double leafY = topMarginPx + ( (startingLeafUnit + (leafUnits * 0.5)) * verticalLeafSpace );
						phyloLeafData.put(SUBTREE_X, leafX);
						phyloLeafData.put(SUBTREE_Y, leafY);
						double collapsedWidthPx = ((maxBranchDepth - minBranchDepth) * branchLengthMultiplier);
						double collapsedHeightPx = leafUnits * verticalLeafSpace;
						phyloLeafData.put(COLLAPSED_WIDTH_PX, collapsedWidthPx);
						phyloLeafData.put(COLLAPSED_HEIGHT_PX, collapsedHeightPx);
					} else {
						double leafBranchDepth = (Double) phyloLeafData.get(LEAF_BRANCH_DEPTH);
						double leafX = xOffset + (leafBranchDepth * branchLengthMultiplier);
						double leafY = topMarginPx + ( (startingLeafUnit + 0.5) * verticalLeafSpace );
						phyloLeafData.put(SUBTREE_X, leafX);
						phyloLeafData.put(SUBTREE_Y, leafY);
					}
				}
			}
		});
	}

	private void generateObjects(CommandContext cmdContext, TreeVisualiser treeVisualiser, PhyloTree phyloTree, 
			double rightMarginPx, double rootLengthPx, double topMarginPx, 
			CommandDocument visDocument) {

		CommandArray leafNodesArray = visDocument.setArray(LEAF_NODES);
		CommandArray internalNodesArray = visDocument.setArray(INTERNAL_NODES);
		CommandArray branchesArray = visDocument.setArray(BRANCHES);
		CommandArray collapsedSubtreesArray = visDocument.setArray(COLLAPSED_SUBTREES);
		
		CommandObject rootObj = visDocument.setObject(ROOT);
		
		phyloTree.accept(new PhyloTreeVisitor() {

			PhyloInternal currentCollapsed = null;

			@Override
			public void preVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == null) {
					if(isCollapsed(phyloInternal)) {
						currentCollapsed = phyloInternal;
					}
				}
			}
			
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				if(currentCollapsed == null) {
					Map<String, Object> leafUserData = phyloLeaf.getUserData();
					double x = (double) leafUserData.get(SUBTREE_X);
					double y = (double) leafUserData.get(SUBTREE_Y);
					if(isCollapsed(phyloLeaf)) {
						CommandObject collapsedObj = collapsedSubtreesArray.addObject();
						double width = (double) leafUserData.get(COLLAPSED_WIDTH_PX);
						double height = ((double) leafUserData.get(COLLAPSED_HEIGHT_PX)) * COLLAPSED_SUBTREE_VERTICAL_PROPORTION;
						collapsedObj.setDouble("rootX", x);
						collapsedObj.setDouble("rootY", y);
						collapsedObj.setDouble("leafX", x + width);
						collapsedObj.setDouble("upperLeafY", y - (height/2));
						collapsedObj.setDouble("lowerLeafY", y + (height/2));
						
						CommandObject collapsedPropertiesObj = collapsedObj.setObject("properties");
						String collapsedText = (String) leafUserData.get(COLLAPSED_TEXT);
						collapsedPropertiesObj.set(COLLAPSED_TEXT, collapsedText);
						double collapsedTextWidthPx = (Double) leafUserData.get(COLLAPSED_TEXT_WIDTH_PX);
						collapsedPropertiesObj.set(COLLAPSED_TEXT_WIDTH_PX, collapsedTextWidthPx);
					} else {
						CommandObject leafObj = leafNodesArray.addObject();
						leafObj.setDouble("x", x);
						leafObj.setDouble("y", y);
						
						CommandObject leafPropertiesObj = leafObj.setObject("properties");
						String phyloLeafName = phyloLeaf.getName();
						leafPropertiesObj.set("name", phyloLeafName);
						Object highlightedValue = leafUserData.get("treevisualiser-highlighted");
						if(highlightedValue != null && highlightedValue.equals("true")) {
							leafObj.setBoolean("highlighted", true);
						}
						String leafText = (String) leafUserData.get(LEAF_TEXT);
						leafPropertiesObj.set(LEAF_TEXT, leafText);
						double leafTextWidthPx = (Double) leafUserData.get(LEAF_TEXT_WIDTH_PX);
						leafPropertiesObj.set(LEAF_TEXT_WIDTH_PX, leafTextWidthPx);
					}
				}
			}
			
			
			@Override
			public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				if(currentCollapsed == null) {
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
			}
			@Override
			public void postVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == null) {
					Map<String, Object> internalUserData = phyloInternal.getUserData();
					double x = (double) internalUserData.get(SUBTREE_X);
					double y = (double) internalUserData.get(SUBTREE_Y);
					CommandObject internalNodeObj = internalNodesArray.addObject();
					internalNodeObj.setDouble("x", x);
					internalNodeObj.setDouble("y", y);
				} else if(currentCollapsed == phyloInternal) {
					Map<String, Object> internalUserData = phyloInternal.getUserData();
					double x = (double) internalUserData.get(SUBTREE_X);
					double y = (double) internalUserData.get(SUBTREE_Y);
					CommandObject collapsedObj = collapsedSubtreesArray.addObject();
					double width = (double) internalUserData.get(COLLAPSED_WIDTH_PX);
					double height = ((double) internalUserData.get(COLLAPSED_HEIGHT_PX)) * COLLAPSED_SUBTREE_VERTICAL_PROPORTION;
					collapsedObj.setDouble("rootX", x);
					collapsedObj.setDouble("rootY", y);
					collapsedObj.setDouble("leafX", x + width);
					collapsedObj.setDouble("upperLeafY", y - (height/2));
					collapsedObj.setDouble("lowerLeafY", y + (height/2));
					
					CommandObject collapsedPropertiesObj = collapsedObj.setObject("properties");
					String collapsedText = (String) internalUserData.get(COLLAPSED_TEXT);
					collapsedPropertiesObj.set(COLLAPSED_TEXT, collapsedText);
					double collapsedTextWidthPx = (Double) internalUserData.get(COLLAPSED_TEXT_WIDTH_PX);
					collapsedPropertiesObj.set(COLLAPSED_TEXT_WIDTH_PX, collapsedTextWidthPx);

					
					currentCollapsed = null;
				}
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

	// find the minimum acceptable branch length multiplier across all collapsed / leaf nodes.	
	private Double getBranchLengthMultiplier(PhyloTree phyloTree, double treeWidthPx, double leafTextGapPx) {
		return phyloTree.accept(new PhyloTreeSummariser<Double>() {
			private double branchLengthMultiplier = Double.MAX_VALUE;
			
			PhyloInternal currentCollapsed = null;
			
			@Override
			public void preVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == null) {
					if(isCollapsed(phyloInternal)) {
						currentCollapsed = phyloInternal;
						Map<String, Object> internalUserData = phyloInternal.getUserData();
						Double collapsedTextWidthPx = (Double) internalUserData.get(COLLAPSED_TEXT_WIDTH_PX);
						double maxRemainingPixelsForNode = treeWidthPx - (leafTextGapPx + collapsedTextWidthPx);
						double maxBranchDepth = (Double) internalUserData.get(MAX_BRANCH_DEPTH);
						double maxBranchLengthMultiplierForNode = maxRemainingPixelsForNode / maxBranchDepth;
						this.branchLengthMultiplier = Math.min(branchLengthMultiplier, maxBranchLengthMultiplierForNode);
					}
				}
			}
			@Override
			public void postVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == phyloInternal) {
					currentCollapsed = null;
				}
			}
			
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				if(currentCollapsed == null) {
					Map<String, Object> leafUserData = phyloLeaf.getUserData();
					if(isCollapsed(phyloLeaf)) {
						Double collapsedTextWidthPx = (Double) leafUserData.get(COLLAPSED_TEXT_WIDTH_PX);
						double maxRemainingPixelsForLeaf = treeWidthPx - (leafTextGapPx + collapsedTextWidthPx);
						double maxBranchDepth = (Double) leafUserData.get(MAX_BRANCH_DEPTH);
						double maxBranchLengthMultiplierForLeaf = maxRemainingPixelsForLeaf / maxBranchDepth;
						this.branchLengthMultiplier = Math.min(branchLengthMultiplier, maxBranchLengthMultiplierForLeaf);
					} else {
						Double leafTextWidthPx = (Double) leafUserData.get(LEAF_TEXT_WIDTH_PX);
						// maximum amount of horizontal space in pixels, which could be used for the branches
						// leading to this leaf
						double maxRemainingPixelsForLeaf = treeWidthPx - (leafTextGapPx + leafTextWidthPx);
						double leafBranchDepth = (Double) leafUserData.get(LEAF_BRANCH_DEPTH);
						double maxBranchLengthMultiplierForLeaf = maxRemainingPixelsForLeaf / leafBranchDepth;
						this.branchLengthMultiplier = Math.min(branchLengthMultiplier, maxBranchLengthMultiplierForLeaf);
					}
				}
			}
			@Override
			public Double get() {
				return branchLengthMultiplier;
			}
		}).get();
	}
	
	// each leaf outside of a collapsed node is allocated 1 leaf unit.
	// collapsed nodes are allocated a fixed number of leaf units (for now)

	// this method sets the STARTING_LEAF_UNIT user data integer on leaf nodes outside 
	// collapsed internals and on collapsed internal/leaf nodes
	// and the LEAF_UNITS user data integer on collapsed internal/leaf nodes
	
	private Integer allocateLeafUnits(PhyloTree phyloTree, int minCollapsedLeafUnits, int maxCollapsedLeafUnits) {
		return phyloTree.accept(new PhyloTreeSummariser<Integer>() {
			Integer numLeafUnits = 0;
			PhyloInternal currentCollapsed = null;
			Integer currentCollapsedLeafNodes = 0;
			
			@Override
			public void preVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == null) {
					if(isCollapsed(phyloInternal)) {
						phyloInternal.ensureUserData().put(STARTING_LEAF_UNIT, numLeafUnits);
						currentCollapsed = phyloInternal;
						currentCollapsedLeafNodes = 0;
					}
				}
			}
			@Override
			public void postVisitInternal(PhyloInternal phyloInternal) {
				if(currentCollapsed == phyloInternal) {
					currentCollapsed = null;
					int collapsedLeafUnits = (int) Math.ceil(Math.log(currentCollapsedLeafNodes)/Math.log(2));
					collapsedLeafUnits = Math.max(collapsedLeafUnits, minCollapsedLeafUnits);
					collapsedLeafUnits = Math.min(collapsedLeafUnits, maxCollapsedLeafUnits);
					numLeafUnits = numLeafUnits + collapsedLeafUnits;
					phyloInternal.ensureUserData().put(LEAF_UNITS, collapsedLeafUnits);
				}
			}

			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				if(currentCollapsed == null) {
					phyloLeaf.ensureUserData().put(STARTING_LEAF_UNIT, numLeafUnits);
					if(isCollapsed(phyloLeaf)) {
						numLeafUnits = numLeafUnits + 1;
						phyloLeaf.ensureUserData().put(LEAF_UNITS, 1);
					} else {
						numLeafUnits = numLeafUnits+1;
					}
				} else {
					currentCollapsedLeafNodes++;
				}
			}
			@Override
			public Integer get() {
				return numLeafUnits;
			}
			
		}).get();
	}
	
	public interface PhyloTreeSummariser<C> extends PhyloTreeVisitor, Supplier<C> {}

	private boolean isCollapsed(PhyloSubtree<?> phyloSubtree) {
		Object collapsed = phyloSubtree.ensureUserData().get("treevisualiser-collapsed");
		return (collapsed != null && collapsed.equals("true"));
	}
	
}
