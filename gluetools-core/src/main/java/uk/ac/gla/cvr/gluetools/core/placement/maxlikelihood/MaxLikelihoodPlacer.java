package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate.OrderStrategy;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlaceNamePQuery;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlacePlacement;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeafLister;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporter;
import uk.ac.gla.cvr.gluetools.programs.mafft.MafftRunner;
import uk.ac.gla.cvr.gluetools.programs.mafft.add.MafftResult;
import uk.ac.gla.cvr.gluetools.programs.raxml.epa.RaxmlEpaResult;
import uk.ac.gla.cvr.gluetools.programs.raxml.epa.RaxmlEpaRunner;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="maxLikelihoodPlacer")
public class MaxLikelihoodPlacer extends ModulePlugin<MaxLikelihoodPlacer> {

	// the root alignment which will provide the phylogeny
	public static final String PHYLO_ALIGNMENT_NAME = "phyloAlignmentName";
	// the field of the alignment table where the phylogeny is stored.
	public static final String PHYLO_FIELD_NAME = "phyloFieldName";
	// the alignment object which will provide the alignment
	public static final String ALIGNMENT_ALIGNMENT_NAME = "alignmentAlignmentName";
	// the feature loc which will restrict the alginment
	public static final String ALIGNMENT_RELATED_REF_NAME = "alignmentRelatedRefName";
	public static final String ALIGNMENT_FEATURE_NAME = "alignmentFeatureName";
	
	private static final String PHYLO_SUBTREE_ALIGNMENT_NAMES_KEY = "alignments";
	private static final String PHYLO_SUBTREE_MEMBER_PK_MAP_KEY = "member";
	
	private String phyloAlignmentName;
	private String phyloFieldName;
	private String alignmentAlignmentName;
	private String alignmentRelatedRefName;
	private String alignmentFeatureName;
	
	private MafftRunner mafftRunner = new MafftRunner();
	private RaxmlEpaRunner raxmlEpaRunner = new RaxmlEpaRunner();
	
	public MaxLikelihoodPlacer() {
		super();
		addModulePluginCmdClass(PlaceSequenceCommand.class);
		addModulePluginCmdClass(PlaceFileCommand.class);
		addSimplePropertyName(PHYLO_ALIGNMENT_NAME);
		addSimplePropertyName(PHYLO_FIELD_NAME);
		addSimplePropertyName(ALIGNMENT_ALIGNMENT_NAME);
		addSimplePropertyName(ALIGNMENT_RELATED_REF_NAME);
		addSimplePropertyName(ALIGNMENT_FEATURE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.phyloAlignmentName = PluginUtils.configureStringProperty(configElem, PHYLO_ALIGNMENT_NAME, true);
		this.phyloFieldName = PluginUtils.configureStringProperty(configElem, PHYLO_FIELD_NAME, true);
		this.alignmentAlignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_ALIGNMENT_NAME, true);
		this.alignmentRelatedRefName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_RELATED_REF_NAME, false);
		this.alignmentFeatureName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_FEATURE_NAME, false);
		
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

		Alignment phyloAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(phyloAlignmentName), true);
		if(phyloAlignment == null) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "No such alignment \""+phyloAlignmentName+"\"");
		}
		if(!phyloAlignment.isConstrained()) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "The phyloAlignment \""+phyloAlignmentName+"\" must be constrained");
		}
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		project.checkProperty(ConfigurableTable.alignment.name(), phyloFieldName, FieldType.VARCHAR, true);

		Alignment alignmentAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentAlignmentName), true);
		if(alignmentAlignment == null) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "No such alignment \""+alignmentAlignment+"\"");
		}
		if((alignmentRelatedRefName != null && alignmentFeatureName == null) || (alignmentRelatedRefName == null && alignmentFeatureName != null)) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "Either both <alignmentRelatedRefName> and <alignmentFeatureName> should be specifed, or neither");
		}
		if(alignmentRelatedRefName != null) {
			phyloAlignment.getRelatedRef(cmdContext, alignmentRelatedRefName);
			GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(alignmentRelatedRefName, alignmentFeatureName));
		}

	}

	public Map<String, List<PlacementResult>> place(CommandContext cmdContext, Map<String, DNASequence> querySequenceMap, File dataDirFile) {
		
		// 
		Alignment phyloAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(phyloAlignmentName));
		Alignment alignmentAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentAlignmentName));
		PhyloTree glueAlmtPhyloTree = PhyloExporter.exportAlignmentPhyloTree(cmdContext, phyloAlignment, phyloFieldName, true);

		PhyloLeafLister phyloLeafLister = new PhyloLeafLister();
		glueAlmtPhyloTree.accept(phyloLeafLister);
		
		Map<Map<String,String>, Map<String,String>> almtMembPkMapToPhyloMembPkMap = new LinkedHashMap<Map<String,String>, Map<String,String>>();
		
		phyloLeafLister.getPhyloLeaves().stream()
				.map(phyLeaf -> phyLeaf.getName())
				.map(name -> Project.targetPathToPkMap(ConfigurableTable.alignment_member, name))
				.forEach(phyloMembPkMap -> {
					Map<String,String> almtMembPkMap = new LinkedHashMap<String,String>(phyloMembPkMap);
					almtMembPkMapToPhyloMembPkMap.put(almtMembPkMap, phyloMembPkMap);
				});
		List<AlignmentMember> almtAlmtMembers = almtMembPkMapToPhyloMembPkMap.keySet().stream()
				.map(memberPkMap -> GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap))
				.collect(Collectors.toList());
		List<AlignmentMember> phyloAlmtMembers = almtMembPkMapToPhyloMembPkMap.values().stream()
				.map(memberPkMap -> GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap))
				.collect(Collectors.toList());

		
		// could make some of these things configurable if necessary
		boolean includeAllColumns = false;
		Integer minColUsage = null;
		OrderStrategy orderStrategy = null;

		Map<Map<String,String>, DNASequence> almtMemberPkMapToAlignmentRow = 
				FastaAlignmentExporter.exportAlignment(cmdContext, 
						alignmentRelatedRefName, alignmentFeatureName, includeAllColumns, minColUsage, orderStrategy, 
						alignmentAlignment, almtAlmtMembers);

		// rename each row to its phylo equivalent.
		Map<Map<String,String>, DNASequence> phyloMemberPkMapToAlignmentRow = new LinkedHashMap<Map<String,String>, DNASequence>();
		almtMemberPkMapToAlignmentRow.forEach((almtMemberPkMap, almtRow) -> {
			phyloMemberPkMapToAlignmentRow.put(almtMembPkMapToPhyloMembPkMap.get(almtMemberPkMap), almtRow);
		});

		// we rename query and member sequences (a) to avoid clashes and (b) to work around program ID limitations.
		Map<String, Map<String,String>> rowNameToMemberPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<Map<String,String>, String> memberPkMapToRowName = new LinkedHashMap<Map<String,String>, String>();
		Map<String, DNASequence> almtFastaContent = FastaUtils.remapFasta(
				phyloMemberPkMapToAlignmentRow, rowNameToMemberPkMap, memberPkMapToRowName, "R");
		
		Map<String, String> rowNameToQueryMap = new LinkedHashMap<String, String>();
		Map<String, String> queryToRowNameMap = new LinkedHashMap<String, String>();
		Map<String, DNASequence> queryFastaContent = FastaUtils.remapFasta(
				querySequenceMap, rowNameToQueryMap, queryToRowNameMap, "Q");
		
		// Maybe this should be ADD_KEEPLENGTH, given that any columns that the query has inserted
		// relative to the reference alignment are basically going to be ignored.
		MafftResult mafftResult = mafftRunner.executeMafft(cmdContext, MafftRunner.Task.ADD, almtFastaContent, queryFastaContent, dataDirFile);
		
		Map<String, DNASequence> alignmentWithQuery = mafftResult.getAlignmentWithQuery();

		
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
				Double distalLength = getDouble(placement.getFieldValues(), distalLengthIndex);
				Double pendantLength = getDouble(placement.getFieldValues(), pendantLengthIndex);
				PhyloBranch jPlacePlacementBranch = labelToJPlaceBranch.get(edgeNum);

				MemberSearchNode nearestMemberNode = findNearestMember(jPlacePlacementBranch, new BigDecimal(distalLength), new BigDecimal(pendantLength));
				PhyloSubtree<?> jPlaceMemberLeaf = nearestMemberNode.phyloSubtree;
				PhyloLeaf glueAlmtMemberLeaf = (PhyloLeaf) jPlaceMemberLeaf.getUserData().get(PhyloTreeReconciler.GLUE_ALMT_PHYLO_OBJ);
				PlacementResult placementResult = new PlacementResult();
				placementResult.setSequenceName(seqName);
				placementResult.setClosestMemberPkMap(
						(Map<String, String>) glueAlmtMemberLeaf.getUserData().get(PHYLO_SUBTREE_MEMBER_PK_MAP_KEY));
				placementResult.setDistanceToClosestMember(nearestMemberNode.totalLength.doubleValue());
				
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
		BigDecimal awayFromRootLength = (branchLength.subtract(towardsRootLength)).add(pendantLength);
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


	
	// collects branch labels from the JPlace tree and stores them in a map from label to branch object
	private class PhyloTreeBranchLabelCollector implements PhyloTreeVisitor {

		private Map<Integer, PhyloBranch> labelToJPlaceBranch = new LinkedHashMap<Integer, PhyloBranch>();
		
		@Override
		public void preVisitBranch(int branchIndex, PhyloBranch jPlacePhyloBranch) {
			String branchLabelString = jPlacePhyloBranch.getBranchLabel();
			if(branchLabelString == null) {
				throw new MaxLikelihoodPlacerException(Code.JPLACE_BRANCH_LABEL_ERROR, "Expected jPlace branch to have a branch label (within '{' and '}')");
			}
			Integer branchLabel = null;
			try {
				branchLabel = Integer.parseInt(branchLabelString);
			} catch(NumberFormatException nfe) {
				throw new MaxLikelihoodPlacerException(Code.JPLACE_BRANCH_LABEL_ERROR, "Expected jPlace branch to have an integer branch label");
			}
			labelToJPlaceBranch.put(branchLabel, jPlacePhyloBranch);
		}

		public Map<Integer, PhyloBranch> getLabelToJPlaceBranch() {
			return labelToJPlaceBranch;
		}
	}

	
	
}
