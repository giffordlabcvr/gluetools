package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExportCommandDelegate.OrderStrategy;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FastaAlignmentExporter;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.SimpleAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.ExplicitMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlaceNamePQuery;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlacePlacement;
import uk.ac.gla.cvr.gluetools.core.jplace.JPlaceResult;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.newick.NewickJPlaceToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeafLister;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeReconciler;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector.AlignmentColumnsSelector;
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
	// the feature loc which will restrict the alignment
	public static final String ALIGNMENT_RELATED_REF_NAME = "alignmentRelatedRefName";
	public static final String ALIGNMENT_FEATURE_NAME = "alignmentFeatureName";
	public static final String SELECTOR_NAME = "selectorName";
	
	private String phyloAlignmentName;
	private String phyloFieldName;
	private String alignmentAlignmentName;
	private String alignmentRelatedRefName;
	private String alignmentFeatureName;
	private String selectorName;
	
	
	private MafftRunner mafftRunner = new MafftRunner();
	private RaxmlEpaRunner raxmlEpaRunner = new RaxmlEpaRunner();
	
	public MaxLikelihoodPlacer() {
		super();
		addModulePluginCmdClass(PlaceSequenceCommand.class);
		addModulePluginCmdClass(PlaceFileCommand.class);
		addModulePluginCmdClass(ListQueryCommand.class);
		addModulePluginCmdClass(ListPlacementCommand.class);
		addModulePluginCmdClass(ExportPlacementPhylogenyCommand.class);
		addModulePluginCmdClass(ListNeighbourCommand.class);
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
		this.selectorName = PluginUtils.configureStringProperty(configElem, SELECTOR_NAME, false);
		
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
		project.checkProperty(ConfigurableTable.alignment.name(), phyloFieldName, EnumSet.of(FieldType.VARCHAR, FieldType.CLOB), true);

		Alignment alignmentAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentAlignmentName), true);
		if(alignmentAlignment == null) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "No such alignment \""+alignmentAlignment+"\"");
		}
		
		if(selectorName != null && (this.alignmentRelatedRefName != null || this.alignmentFeatureName != null)) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "If <selectorName> is specified then neither <alignmentRelatedRefName> nor <alignmentFeatureName> may be used");
		}
		if((alignmentRelatedRefName != null && alignmentFeatureName == null) || (alignmentRelatedRefName == null && alignmentFeatureName != null)) {
			throw new MaxLikelihoodPlacerException(Code.CONFIG_ERROR, "Either both <alignmentRelatedRefName> and <alignmentFeatureName> should be specifed, or neither");
		}
		if(alignmentRelatedRefName != null) {
			phyloAlignment.getRelatedRef(cmdContext, alignmentRelatedRefName);
			GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(alignmentRelatedRefName, alignmentFeatureName));
		}

	}

	public PlacerResultInternal place(CommandContext cmdContext, Map<String, DNASequence> querySequenceMap, File dataDirFile) {
		PhyloTree glueProjectPhyloTree = constructGlueProjectPhyloTree(cmdContext);
		return place(cmdContext, glueProjectPhyloTree, querySequenceMap, dataDirFile);
	}		
	
	public PlacerResultInternal place(CommandContext cmdContext, PhyloTree glueProjectPhyloTree, Map<String, DNASequence> querySequenceMap, File dataDirFile) {
		if(querySequenceMap.isEmpty()) {
			throw new MaxLikelihoodPlacerException(Code.INPUT_ERROR, "No sequences found");
		}
		
		Alignment alignmentAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentAlignmentName));

		// phylo tree leaves reference constrained alignment members
		// however, the alignment passed to MAFFT/EPA may be a different, unconstrained alignment.
		// We will manage this by building a mapping between alignment member pkMaps.
		Map<Map<String,String>, Map<String,String>> almtMembPkMapToPhyloMembPkMap = new LinkedHashMap<Map<String,String>, Map<String,String>>();

		PhyloLeafLister phyloLeafLister = new PhyloLeafLister();
		glueProjectPhyloTree.accept(phyloLeafLister);
		
		phyloLeafLister.getPhyloLeaves().stream()
				.map(phyLeaf -> phyLeaf.getName())
				.map(name -> Project.targetPathToPkMap(ConfigurableTable.alignment_member, name))
				.forEach(phyloMembPkMap -> {
					// copy the pkMap and change the alignment name
					Map<String,String> almtMembPkMap = new LinkedHashMap<String,String>(phyloMembPkMap);
					almtMembPkMap.put(AlignmentMember.ALIGNMENT_NAME_PATH, alignmentAlignment.getName());
					// record the correspondence.
					almtMembPkMapToPhyloMembPkMap.put(almtMembPkMap, phyloMembPkMap);
				});
		
		// could make some of these things configurable if necessary, for example if we start using constrained alignments.
		OrderStrategy orderStrategy = null;
		boolean excludeEmptyRows = false;

		IAlignmentColumnsSelector alignmentColumnsSelector;
		if(selectorName != null) {
			alignmentColumnsSelector = Module.resolveModulePlugin(cmdContext, AlignmentColumnsSelector.class, selectorName);
		} else if(alignmentRelatedRefName != null && alignmentFeatureName != null) {
			alignmentColumnsSelector = new SimpleAlignmentColumnsSelector(alignmentRelatedRefName, alignmentFeatureName, null, null, null, null);
		} else {
			alignmentColumnsSelector = null;
		}
		ExplicitMemberSupplier explicitMemberSupplier 
			= new ExplicitMemberSupplier(alignmentAlignment.getName(), new ArrayList<Map<String,String>>(almtMembPkMapToPhyloMembPkMap.keySet()));
		
		Map<Map<String,String>, DNASequence> almtMemberPkMapToAlignmentRow = 
				FastaAlignmentExporter.exportAlignment(cmdContext, 
						alignmentColumnsSelector, excludeEmptyRows, orderStrategy, 
						explicitMemberSupplier);

		// rename each row to its phylo member equivalent.
		Map<Map<String,String>, DNASequence> phyloMemberPkMapToAlignmentRow = new LinkedHashMap<Map<String,String>, DNASequence>();
		almtMemberPkMapToAlignmentRow.forEach((almtMemberPkMap, almtRow) -> {
			phyloMemberPkMapToAlignmentRow.put(almtMembPkMapToPhyloMembPkMap.get(almtMemberPkMap), almtRow);
		});

		// from this point on, phylo tree alignment members are used.
		
		// when running MAFFT/EPA we rename query and member sequences (a) to avoid clashes and (b) to work around program ID limitations.
		Map<String, Map<String,String>> rowNameToMemberPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<Map<String,String>, String> memberPkMapToRowName = new LinkedHashMap<Map<String,String>, String>();
		Map<String, DNASequence> almtFastaContent = FastaUtils.remapFasta(
				phyloMemberPkMapToAlignmentRow, rowNameToMemberPkMap, memberPkMapToRowName, "R");
		
		Map<String, String> rowNameToQueryMap = new LinkedHashMap<String, String>();
		Map<String, String> queryToRowNameMap = new LinkedHashMap<String, String>();
		Map<String, DNASequence> queryFastaContent = FastaUtils.remapFasta(
				querySequenceMap, rowNameToQueryMap, queryToRowNameMap, "Q");
		
		MafftResult mafftResult = mafftRunner.executeMafft(cmdContext, MafftRunner.Task.ADD_KEEPLENGTH, true, almtFastaContent, queryFastaContent, dataDirFile);
		
		Map<String, DNASequence> alignmentWithQuery = mafftResult.getAlignmentWithQuery();

		// add special EPA names to leaves 
		// in the glue alignment tree so that they match the alignment rows when it is passed to Raxml EPA
		glueProjectPhyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafName = phyloLeaf.getName();
				Map<String,String> phyloMemberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, leafName);
				String epaLeafName = memberPkMapToRowName.get(phyloMemberPkMap);
				phyloLeaf.ensureUserData().put(RaxmlEpaRunner.EPA_LEAF_NAME_USER_DATA_KEY, epaLeafName);
			}
		});
		RaxmlEpaResult raxmlEpaResult = raxmlEpaRunner.executeRaxmlEpa(cmdContext, glueProjectPhyloTree, alignmentWithQuery, dataDirFile);
		JPlaceResult jPlaceResult = raxmlEpaResult.getjPlaceResult();

		PhyloTree jPlacePhyloTree = jPlaceResult.getTree();

		// rename jPlace result tree leaves back to their reference sequence phylo member names
		jPlacePhyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				Map<String, String> memberPkMap = rowNameToMemberPkMap.get(phyloLeaf.getName());
				if(memberPkMap == null) {
					throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
							"JPlace leaf contains unknown row name "+phyloLeaf.getName());
				}
				phyloLeaf.setName(Project.pkMapToTargetPath(ConfigurableTable.alignment_member.getModePath(), memberPkMap));
			}
		});
		
		return new PlacerResultInternal(jPlacePhyloTree, glueProjectPhyloTree, getSingleQueryResults(jPlaceResult, rowNameToQueryMap));
	}

	public static class PlacerResultInternal {
		private PhyloTree labelledPhyloTree;
		private Map<String, MaxLikelihoodSingleQueryResult> queryResults;
		private Map<Integer, PhyloBranch> edgeIndexToPhyloBranch;
		
		private PlacerResultInternal(PhyloTree labelledPhyloTree, 
				PhyloTree glueProjectPhyloTree, Map<String, MaxLikelihoodSingleQueryResult> queryResults) {
			super();
			this.labelledPhyloTree = labelledPhyloTree;
			this.edgeIndexToPhyloBranch = generateEdgeIndexToPhyloBranch(labelledPhyloTree, glueProjectPhyloTree);
			this.queryResults = queryResults;
		}

		public PhyloTree getLabelledPhyloTree() {
			return labelledPhyloTree;
		}

		public Map<String, MaxLikelihoodSingleQueryResult> getQueryResults() {
			return queryResults;
		}

		public Map<Integer, PhyloBranch> getEdgeIndexToPhyloBranch() {
			return edgeIndexToPhyloBranch;
		}

		public MaxLikelihoodPlacerResult toPojoResult() {
			MaxLikelihoodPlacerResult maxLikelihoodPlacerResult = new MaxLikelihoodPlacerResult();
			PhyloFormat resultPhyloFormat = PhyloFormat.NEWICK_JPLACE;
			maxLikelihoodPlacerResult.labelledPhyloTreeFormat = resultPhyloFormat.name();
			maxLikelihoodPlacerResult.labelledPhyloTree = new String(resultPhyloFormat.generate(labelledPhyloTree));
			maxLikelihoodPlacerResult.singleQueryResult = new ArrayList<MaxLikelihoodSingleQueryResult>(queryResults.values());
			return maxLikelihoodPlacerResult;
		}
		
	}
	
	public PhyloTree constructGlueProjectPhyloTree(CommandContext cmdContext) {
		Alignment phyloAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(phyloAlignmentName));
		PhyloTree glueAlmtPhyloTree = PhyloExporter.exportAlignmentPhyloTree(cmdContext, phyloAlignment, phyloFieldName, true);
		return glueAlmtPhyloTree;
	}

	private Map<String, MaxLikelihoodSingleQueryResult> getSingleQueryResults(JPlaceResult jPlaceResult, Map<String, String> rowNameToQueryMap) {
		Map<String, MaxLikelihoodSingleQueryResult> singleQueryResults = new LinkedHashMap<String, MaxLikelihoodSingleQueryResult>();
		
		Map<String, List<JPlacePlacement>> queryNameToJPlacePlacements = extractJPlacePlacements(jPlaceResult, rowNameToQueryMap);
		List<String> fields = jPlaceResult.getFields();
		
		int edgeNumIndex = findIndex(fields, "edge_num");
		int logLikelihoodIndex = findIndex(fields, "likelihood");
		int likeWeightRatioIndex = findIndex(fields, "like_weight_ratio"); 
		int distalLengthIndex = findIndex(fields, "distal_length");
		int pendantLengthIndex = findIndex(fields, "pendant_length");

		for(String queryName: rowNameToQueryMap.values()) {
			List<JPlacePlacement> jPlacePlacements = queryNameToJPlacePlacements.get(queryName);
			if(jPlacePlacements == null) {
				throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
						"No JPlace placements found for query sequence "+queryName);
			}
			MaxLikelihoodSingleQueryResult singleQueryResult = new MaxLikelihoodSingleQueryResult();
			singleQueryResult.queryName = queryName;
			
			int placementIndex = 1;
			for(JPlacePlacement jPlacePlacement: jPlacePlacements) {
				MaxLikelihoodSinglePlacement singlePlacement = new MaxLikelihoodSinglePlacement();
				singlePlacement.placementIndex = placementIndex;
				singlePlacement.edgeIndex = getInt(jPlacePlacement.getFieldValues(), edgeNumIndex);
				singlePlacement.logLikelihood = getDouble(jPlacePlacement.getFieldValues(), logLikelihoodIndex);
				singlePlacement.distalLength = getDouble(jPlacePlacement.getFieldValues(), distalLengthIndex);
				singlePlacement.pendantLength = getDouble(jPlacePlacement.getFieldValues(), pendantLengthIndex);
				singlePlacement.likeWeightRatio = getDouble(jPlacePlacement.getFieldValues(), likeWeightRatioIndex);
				singleQueryResult.singlePlacement.add(singlePlacement);
				placementIndex++;
			}
			singleQueryResults.put(queryName, singleQueryResult);
		}
		return singleQueryResults;
		
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

	private Map<String, List<JPlacePlacement>> extractJPlacePlacements(JPlaceResult jPlaceResult, Map<String, String> rowNameToQueryMap) {
		Map<String, List<JPlacePlacement>> seqNameToPlacements = new LinkedHashMap<String, List<JPlacePlacement>>();
		
		jPlaceResult.getPQueries().forEach(pQuery -> {
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
	
	public static Map<Integer, PhyloBranch> generateEdgeIndexToPhyloBranch(PhyloTree labelledPhyloTree, PhyloTree glueProjectPhyloTree) {
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = new LinkedHashMap<Integer, PhyloBranch>();
		
		// reconcile labelled phylo tree with GLUE project tree.
		PhyloTreeReconciler phyloTreeReconciler = new PhyloTreeReconciler(glueProjectPhyloTree);
		labelledPhyloTree.accept(phyloTreeReconciler);
		Map<PhyloObject<?>, PhyloObject<?>> labelledToGluePhyloObj = phyloTreeReconciler.getVisitedToSupplied();
		labelledPhyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				Integer branchLabel;
				try {
					branchLabel = (Integer) phyloBranch.ensureUserData().get(NewickJPlaceToPhyloTreeParser.J_PLACE_BRANCH_LABEL);
				} catch(Exception e) {
					throw new MaxLikelihoodPlacerException(MaxLikelihoodPlacerException.Code.JPLACE_STRUCTURE_ERROR, 
							e, "Labelled phylo tree is missing integer branch labels: "+e.getLocalizedMessage());
				}
				edgeIndexToPhyloBranch.put(branchLabel, (PhyloBranch) labelledToGluePhyloObj.get(phyloBranch));
			}
		});
		return edgeIndexToPhyloBranch;
	}


	// adds a single new leaf to the glueProjectPhyloTree based on a specific placement.
	public static PhyloLeaf addPlacementToPhylogeny(
			PhyloTree glueProjectPhyloTree,
			Map<Integer, PhyloBranch> edgeIndexToPhyloBranch,
			MaxLikelihoodSingleQueryResult queryResult,
			MaxLikelihoodSinglePlacement placement) {
		PhyloBranch insertionBranch = edgeIndexToPhyloBranch.get(placement.edgeIndex);
		PhyloSubtree<?> subtree = insertionBranch.getSubtree();

		// new phylo objects
		PhyloInternal newPhyloInternal = new PhyloInternal();
		PhyloBranch phyloBranchToLeaf = new PhyloBranch();
		PhyloBranch phyloBranchToSubtree = new PhyloBranch();
		PhyloLeaf placementLeaf = new PhyloLeaf();

		// change topology
		subtree.setParentPhyloBranch(null);
		insertionBranch.setSubtree(newPhyloInternal);
		phyloBranchToSubtree.setSubtree(subtree);
		phyloBranchToLeaf.setSubtree(placementLeaf);
		newPhyloInternal.addBranch(phyloBranchToSubtree);
		newPhyloInternal.addBranch(phyloBranchToLeaf);

		// branch lengths
		BigDecimal originalInsertionBranchLength = insertionBranch.getLength();
		BigDecimal distalLength = new BigDecimal(placement.distalLength);
		insertionBranch.setLength(distalLength);
		BigDecimal phyloBranchToSubtreeLength = originalInsertionBranchLength.subtract(distalLength);
		if(phyloBranchToSubtreeLength.compareTo(BigDecimal.ZERO) < 0) {
			// Sometimes EPA returns distal lengths which are longer than the insertion branch length.
			// This seems to happen if you query with one of the reference sequences.
			phyloBranchToSubtreeLength = new BigDecimal(0.0);
		}
		phyloBranchToSubtree.setLength(phyloBranchToSubtreeLength);
		phyloBranchToLeaf.setLength(new BigDecimal(placement.pendantLength));
		
		return placementLeaf;
	}

	// resets the glueProjectPhyloTree to the way it was before the placement leaf was added.
	public static void removePlacementFromPhylogeny(PhyloLeaf placementLeaf) {
		PhyloBranch phyloBranchToSubtree = null;
		PhyloBranch phyloBranchToLeaf = placementLeaf.getParentPhyloBranch();
		PhyloInternal newPhyloInternal = phyloBranchToLeaf.getParentPhyloInternal();
		for(PhyloBranch phyloBranch : newPhyloInternal.getBranches()) {
			if(phyloBranch != phyloBranchToLeaf) {
				phyloBranchToSubtree = phyloBranch;
				break;
			}
		}
		PhyloSubtree<?> subtree = phyloBranchToSubtree.getSubtree();
		PhyloBranch insertionBranch = newPhyloInternal.getParentPhyloBranch();

		// restore branch length
		insertionBranch.setLength(insertionBranch.getLength().add(phyloBranchToSubtree.getLength()));

		// restore topology
		subtree.setParentPhyloBranch(null);
		insertionBranch.setSubtree(subtree);
		
	}

	
}
