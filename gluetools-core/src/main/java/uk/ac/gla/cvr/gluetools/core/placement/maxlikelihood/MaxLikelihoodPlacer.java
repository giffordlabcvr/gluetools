package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.cayenne.exp.Expression;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate.OrderStrategy;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlaceNamePQuery;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlacePlacement;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.TreeRenderer;
import uk.ac.gla.cvr.gluetools.core.treerenderer.TreeRendererContext;
import uk.ac.gla.cvr.gluetools.programs.mafft.MafftRunner;
import uk.ac.gla.cvr.gluetools.programs.mafft.add.MafftResult;
import uk.ac.gla.cvr.gluetools.programs.raxml.epa.RaxmlEpaResult;
import uk.ac.gla.cvr.gluetools.programs.raxml.epa.RaxmlEpaRunner;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="maxLikelihoodPlacer")
public class MaxLikelihoodPlacer extends ModulePlugin<MaxLikelihoodPlacer> {

	public static final String ROOT_ALIGNMENT_NAME = "rootAlignmentName";
	public static final String PHYLO_MEMBER_WHERE_CLAUSE = "phyloMemberWhereClause";
	
	private static final String PHYLO_SUBTREE_ALIGNMENT_NAMES_KEY = "alignments";
	private static final String PHYLO_SUBTREE_MEMBER_PK_MAP_KEY = "member";
	
	private String rootAlignmentName;
	private Optional<Expression> phyloMemberWhereClause;
	
	private MafftRunner mafftRunner = new MafftRunner();
	private RaxmlEpaRunner raxmlEpaRunner = new RaxmlEpaRunner();
	
	public MaxLikelihoodPlacer() {
		super();
		addModulePluginCmdClass(PlaceSequenceCommand.class);
		addModulePluginCmdClass(PlaceFileCommand.class);
		addSimplePropertyName(ROOT_ALIGNMENT_NAME);
		addSimplePropertyName(PHYLO_MEMBER_WHERE_CLAUSE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.rootAlignmentName = PluginUtils.configureStringProperty(configElem, ROOT_ALIGNMENT_NAME, true);
		this.phyloMemberWhereClause = 
				Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, PHYLO_MEMBER_WHERE_CLAUSE, false));
		
		Element mafftRunnerElem = PluginUtils.findConfigElement(configElem, "mafftRunner");
		if(mafftRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, mafftRunnerElem, mafftRunner);
		}
		Element raxmlEpaRunnerElem = PluginUtils.findConfigElement(configElem, "raxmlEpaRunner");
		if(raxmlEpaRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, raxmlEpaRunnerElem, raxmlEpaRunner);
		}
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		// check rootAlignment exists and is constrained.
		Alignment rootAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(rootAlignmentName), true);
		if(rootAlignment == null) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "No such alignment \""+rootAlignmentName+"\"");
		}
		if(!rootAlignment.isConstrained()) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "Alignment \""+rootAlignmentName+"\" is unconstrained");
		}
	}

	public Map<String, List<PlacementResult>> place(CommandContext cmdContext, Map<String, DNASequence> querySequenceMap, File dataDirFile) {
		// could make some of these things configurable if necessary
		boolean recursive = true;
		boolean includeAllColumns = true;
		Integer minColUsage = 2;
		boolean deduplicate = true;
		String acRefName = null;
		String featureName = null;
		OrderStrategy orderStrategy = null;
		
		Alignment rootAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(rootAlignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, rootAlignment, recursive, deduplicate, phyloMemberWhereClause);
		Map<Map<String,String>, DNASequence> memberPkMapToAlignmentRow = 
				FastaAlignmentExporter.exportAlignment(cmdContext, 
						acRefName, featureName, includeAllColumns, minColUsage, orderStrategy, 
						rootAlignment, almtMembers);
		
		// we rename query and member sequences (a) to avoid clashes and (b) to work around program ID limitations.
		Map<String, Map<String,String>> rowNameToMemberPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<Map<String,String>, String> memberPkMapToRowName = new LinkedHashMap<Map<String,String>, String>();
		Map<String, DNASequence> almtFastaContent = FastaUtils.remapFasta(
				memberPkMapToAlignmentRow, rowNameToMemberPkMap, memberPkMapToRowName, "R");
		
		Map<String, String> rowNameToQueryMap = new LinkedHashMap<String, String>();
		Map<String, String> queryToRowNameMap = new LinkedHashMap<String, String>();
		Map<String, DNASequence> queryFastaContent = FastaUtils.remapFasta(
				querySequenceMap, rowNameToQueryMap, queryToRowNameMap, "Q");
		
		MafftResult mafftResult = mafftRunner.executeMafft(cmdContext, MafftRunner.Task.ADD, almtFastaContent, queryFastaContent, dataDirFile);
		
		Map<String, DNASequence> alignmentWithQuery = mafftResult.getAlignmentWithQuery();

		PhyloTree glueAlmtPhyloTree = TreeRenderer.phyloTreeFromAlignment(cmdContext, rootAlignment, almtMembers, 
				new MaxLikelihoodTreeRendererContext(memberPkMapToRowName));
		
		RaxmlEpaResult raxmlEpaResult = raxmlEpaRunner.executeRaxmlEpa(cmdContext, glueAlmtPhyloTree, alignmentWithQuery, dataDirFile);

		// reconcile the RAxML jPlace phylo tree with the GLUE alignment phylo tree
		PhyloTreeReconciler phyloTreeReconciler = new PhyloTreeReconciler(glueAlmtPhyloTree);
		PhyloTree jPlacePhyloTree = raxmlEpaResult.getjPlaceResult().getTree();
		jPlacePhyloTree.accept(phyloTreeReconciler);
		
		// Identify the mapping from JPlace integer branch label to JPlace branch.
		PhyloTreeBranchLabelCollector branchLabelCollector = new PhyloTreeBranchLabelCollector();
		jPlacePhyloTree.accept(branchLabelCollector);
		Map<Integer, PhyloBranch> labelToJPlaceBranch = branchLabelCollector.getLabelToJPlaceBranch();
		
		Map<String, List<PlacementResult>> seqNameToPlacementResults = getPlacementResults(cmdContext, querySequenceMap.keySet(), 
				rowNameToQueryMap, labelToJPlaceBranch, raxmlEpaResult, glueAlmtPhyloTree);

		return seqNameToPlacementResults;
	}

	

	@SuppressWarnings("unchecked")
	private Map<String, List<PlacementResult>> getPlacementResults(CommandContext cmdContext, 
			Set<String> querySequenceNames,
			Map<String, String> rowNameToQueryMap, 
			Map<Integer, PhyloBranch> labelToJPlaceBranch,
			RaxmlEpaResult raxmlEpaResult, 
			PhyloTree glueAlmtPhyloTree) {
		
		Map<String, List<PlacementResult>> seqNameToPlacementResult = new LinkedHashMap<String, List<PlacementResult>>();
		
		Map<String, List<JPlacePlacement>> seqNameToPlacements = extractPlacements(rowNameToQueryMap, raxmlEpaResult);
		List<String> fields = raxmlEpaResult.getjPlaceResult().getFields();
		
		int edgeNumIndex = findIndex(fields, "edge_num");
		// log-likelihood: unused at present
		// int likelihoodIndex = findIndex(fields, "likelihood");
		int likeWeightRatioIndex = findIndex(fields, "like_weight_ratio"); 
		int distalLengthIndex = findIndex(fields, "distal_length");
		int pendantLengthIndex = findIndex(fields, "pendant_length");
		
		for(String seqName: querySequenceNames) {
			List<JPlacePlacement> placements = seqNameToPlacements.get(seqName);
			if(placements == null) {
				throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
						"No JPlace placements found for query sequence "+seqName);
			}
			for(JPlacePlacement placement: placements) {
				int edgeNum = getInt(placement.getFieldValues(), edgeNumIndex);
				BigDecimal distalLength = getBigDecimal(placement.getFieldValues(), distalLengthIndex);
				BigDecimal pendantLength = getBigDecimal(placement.getFieldValues(), pendantLengthIndex);
				PhyloBranch jPlacePlacementBranch = labelToJPlaceBranch.get(edgeNum);

				MemberSearchNode nearestMemberNode = findNearestMember(jPlacePlacementBranch, distalLength, pendantLength);
				PhyloSubtree<?> jPlaceMemberLeaf = nearestMemberNode.phyloSubtree;
				PhyloLeaf glueAlmtMemberLeaf = (PhyloLeaf) jPlaceMemberLeaf.getUserData().get(PhyloTreeReconciler.GLUE_ALMT_PHYLO_OBJ);
				PlacementResult placementResult = new PlacementResult();
				placementResult.setSequenceName(seqName);
				placementResult.setClosestMemberPkMap(
						(Map<String, String>) glueAlmtMemberLeaf.getUserData().get(PHYLO_SUBTREE_MEMBER_PK_MAP_KEY));
				placementResult.setDistanceToClosestMember(nearestMemberNode.totalLength);
				
				PhyloBranch glueAlmtPlacementBranch = (PhyloBranch) jPlacePlacementBranch.getUserData().get(PhyloTreeReconciler.GLUE_ALMT_PHYLO_OBJ);
				String groupingAlignmentName = findGroupingAlignmentName(glueAlmtPlacementBranch);
				
				placementResult.setGroupingAlignmentName(groupingAlignmentName);
				
				placementResult.setLikeWeightRatio(getDouble(placement.getFieldValues(), likeWeightRatioIndex));
				
				seqNameToPlacementResult.computeIfAbsent(seqName, 
						sName -> new ArrayList<PlacementResult>()).add(placementResult);
			}
		}
		return seqNameToPlacementResult;
		
	}

	@SuppressWarnings("unchecked")
	private String findGroupingAlignmentName(PhyloBranch glueAlmtPlacementBranch) {
		// walk up the tree to find the grouping alignment
		String groupingAlignmentName = null;
		PhyloBranch currentBranch = glueAlmtPlacementBranch;
		while(currentBranch != null && groupingAlignmentName == null) {
			PhyloInternal currentInternal = currentBranch.getParentPhyloInternal();
			if(currentInternal == null) {
				currentBranch = null;
			} else {
				Map<String, Object> userData = currentInternal.getUserData();
				if(userData != null) {
					List<String> alignmentNames = (List<String>) userData.get(PHYLO_SUBTREE_ALIGNMENT_NAMES_KEY);
					if(alignmentNames != null && !alignmentNames.isEmpty()) {
						// which alignment we pick in this corner case should ultimately depend on distal_length.
						// picking the first alignment is conservative.
						groupingAlignmentName = alignmentNames.get(0); 
					}
				}
				currentBranch = currentInternal.getParentPhyloBranch();
			}
		}
		return groupingAlignmentName;
	}

	private MemberSearchNode findNearestMember(PhyloBranch placementBranch,
			BigDecimal distalLength, BigDecimal pendantLength) {
		LinkedList<MemberSearchNode> nodeQueue = new LinkedList<MemberSearchNode>();
		BigDecimal branchLength = placementBranch.getLength();
		// The two nodes attached to the placement branch.
		PhyloInternal towardsRoot = placementBranch.getParentPhyloInternal();
		PhyloSubtree<?> awayFromRoot = placementBranch.getSubtree();
		BigDecimal towardsRootLength = distalLength.add(pendantLength);
		nodeQueue.add(new MemberSearchNode(towardsRootLength, towardsRoot));
		BigDecimal awayFromRootLength = branchLength.subtract(towardsRootLength).add(pendantLength);
		nodeQueue.add(new MemberSearchNode(awayFromRootLength, awayFromRoot));
		
		Set<PhyloSubtree<?>> visited = new LinkedHashSet<PhyloSubtree<?>>();
		MemberSearchNode bestNode = null;
		while(!nodeQueue.isEmpty()) {
			MemberSearchNode currentNode = nodeQueue.pop();
			visited.add(currentNode.phyloSubtree);
			if(bestNode != null && currentNode.totalLength.compareTo(bestNode.totalLength) >= 0) {
				continue;
			}
			if(currentNode.phyloSubtree instanceof PhyloLeaf) {
				if(bestNode == null || currentNode.totalLength.compareTo(bestNode.totalLength) < 0) {
					bestNode = currentNode;
				}
			} else {
				PhyloInternal currentPhyloInternal = (PhyloInternal) currentNode.phyloSubtree;
				PhyloBranch parentPhyloBranch = currentPhyloInternal.getParentPhyloBranch();
				if(parentPhyloBranch != null) {
					PhyloInternal parentPhyloInternal = parentPhyloBranch.getParentPhyloInternal();
					if(parentPhyloInternal != null && !visited.contains(parentPhyloInternal)) {
						BigDecimal parentBranchLength = parentPhyloBranch.getLength();
						nodeQueue.add(new MemberSearchNode(currentNode.totalLength.add(parentBranchLength), parentPhyloInternal));
					}
				}
				for(PhyloBranch childPhyloBranch: currentPhyloInternal.getBranches()) {
					BigDecimal childBranchLength = childPhyloBranch.getLength();
					nodeQueue.add(new MemberSearchNode(currentNode.totalLength.add(childBranchLength), childPhyloBranch.getSubtree()));
				}
			}
		}
		return bestNode;
	}
	
	private class MemberSearchNode {
		BigDecimal totalLength;
		PhyloSubtree<?> phyloSubtree;
		public MemberSearchNode(BigDecimal totalLength, PhyloSubtree<?> phyloSubtree) {
			super();
			this.totalLength = totalLength;
			this.phyloSubtree = phyloSubtree;
		}
		
	}

	private BigDecimal getBigDecimal(List<Object> values, int index) {
		return getValue(values, BigDecimal.class, index);
	}

	private Double getDouble(List<Object> values, int index) {
		return getValue(values, BigDecimal.class, index).doubleValue();
	}

	private Integer getInt(List<Object> values, int index) {
		return getValue(values, Integer.class, index);
	}

	private <D> D getValue(List<Object> values, Class<D> theClass, int index) {
		if(index > values.size()-1) {
			throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
					"Incorrect number of placement values");
		}
		Object value = values.get(index);
		if(!theClass.isAssignableFrom(value.getClass())) {
			throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
					"Placement value "+index+" is of incorrect type: expected "+theClass.getSimpleName());
		}
		return theClass.cast(value);
	}
	
	private int findIndex(List<String> fields, String fieldName) {
		int index = fields.indexOf(fieldName);
		if(index < 0) {
			throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
					"Could not find placement field "+fieldName);
		}
		return index;
	}

	private Map<String, List<JPlacePlacement>> extractPlacements(Map<String, String> rowNameToQueryMap, RaxmlEpaResult raxmlEpaResult) {
		Map<String, List<JPlacePlacement>> seqNameToPlacements = new LinkedHashMap<String, List<JPlacePlacement>>();
		
		raxmlEpaResult.getjPlaceResult().getPQueries().forEach(pQuery -> {
			if(!(pQuery instanceof JPlaceNamePQuery)) {
				throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
						"Expected JPlace pQueries to be name-based.");
			}
			JPlaceNamePQuery jPlaceNamePQuery = (JPlaceNamePQuery) pQuery;
			List<String> pQueryNames = jPlaceNamePQuery.getNames();
			if(pQueryNames.size() != 1) {
				throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
						"Expected JPlace NamePQuery to contain exactly one name.");
			}
			String rowName = pQueryNames.get(0);
			String seqName = rowNameToQueryMap.get(rowName);
			if(seqName == null) {
				throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
						"Row name \""+rowName+"\" in JPlace result was unrecognized.");
			}
			List<JPlacePlacement> placements = pQuery.getPlacements();
			if(placements.size() == 0) {
				throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
						"Expected JPlace Placements to contain one or more placements.");
			}
			seqNameToPlacements.put(seqName, placements);
		});
		
		return seqNameToPlacements;
	}


	
	// Renderer of alignment to NewickTree, suitable for downstream consumption.
	private class MaxLikelihoodTreeRendererContext extends TreeRendererContext {

		private Map<Map<String, String>, String> memberPkMapToRowName;

		public MaxLikelihoodTreeRendererContext(Map<Map<String, String>, String> memberPkMapToRowName) {
			super();
			this.memberPkMapToRowName = memberPkMapToRowName;
			setForceBifurcating(true);
			setOmitSingleChildInternals(true);
		}

		@Override
		public PhyloInternal phyloInternalForAlignment(
				CommandContext commandContext, Alignment alignment) {
			PhyloInternal phyloInternal = super.phyloInternalForAlignment(commandContext, alignment);
			Map<String, Object> userData = phyloInternal.ensureUserData();
			List<String> alignmentNames = new ArrayList<String>();
			alignmentNames.add(alignment.getName());
			userData.put(PHYLO_SUBTREE_ALIGNMENT_NAMES_KEY, alignmentNames);
			phyloInternal.setUserData(userData);
			return phyloInternal;
		}

		@Override
		public PhyloLeaf phyloLeafForMember(CommandContext commandContext,
				AlignmentMember alignmentMember) {
			PhyloLeaf phyloLeaf = super.phyloLeafForMember(commandContext, alignmentMember);
			Map<String, Object> userData = phyloLeaf.ensureUserData();
			List<String> alignmentNames = new ArrayList<String>();
			userData.put(PHYLO_SUBTREE_ALIGNMENT_NAMES_KEY, alignmentNames);
			userData.put(PHYLO_SUBTREE_MEMBER_PK_MAP_KEY, alignmentMember.pkMap());
			phyloLeaf.setUserData(userData);
			return phyloLeaf;
		}

		// almtPhyloInternal is being optimized away, so we need to save any user data to singleBranchSubtree
		@SuppressWarnings("unchecked")
		@Override
		public Map<String, Object> mergeUserData(
				PhyloObject<?> almtPhyloInternal,
				PhyloObject<?> singleBranchSubtree) {
			Map<String, Object> subtreeUserData = singleBranchSubtree.getUserData();
			Map<String, Object> internalUserData = almtPhyloInternal.getUserData();
			if(subtreeUserData == null) {
				return internalUserData;
			}
			if(internalUserData == null) {
				return subtreeUserData;
			}
			List<String> subtreeAlmtNames = (List<String>) subtreeUserData.get(PHYLO_SUBTREE_ALIGNMENT_NAMES_KEY);
			List<String> internalAlmtNames = (List<String>) internalUserData.get(PHYLO_SUBTREE_ALIGNMENT_NAMES_KEY);
			subtreeAlmtNames.addAll(internalAlmtNames);
			return subtreeUserData;
		}

		@Override
		protected void setAlignmentPhyloInternalName(
				CommandContext commandContext, Alignment alignment,
				PhyloSubtree<?> alignmentPhyloInternal) {
			// no names required.
		}

		@Override
		protected void setMemberPhyloLeafName(CommandContext commandContext,
				AlignmentMember alignmentMember, PhyloLeaf memberPhyloLeaf) {
			memberPhyloLeaf.setName(memberPkMapToRowName.get(alignmentMember.pkMap()));
		}

	}

	// collects branch labels from the JPlace tree and stores them in a map from label to branch object
	private class PhyloTreeBranchLabelCollector implements PhyloTreeVisitor {

		private Map<Integer, PhyloBranch> labelToJPlaceBranch = new LinkedHashMap<Integer, PhyloBranch>();
		
		@Override
		public void preVisitBranch(int branchIndex, PhyloBranch jPlacePhyloBranch) {
			Integer branchLabel = jPlacePhyloBranch.getBranchLabel();
			if(branchLabel == null) {
				throw new MaxLikelihoodPlacerException(Code.JPLACE_BRANCH_LABEL_ERROR, "Expected jPlace branch to have an integer label");
			}
			labelToJPlaceBranch.put(branchLabel, jPlacePhyloBranch);
		}

		public Map<Integer, PhyloBranch> getLabelToJPlaceBranch() {
			return labelToJPlaceBranch;
		}
	}

	
	
}
