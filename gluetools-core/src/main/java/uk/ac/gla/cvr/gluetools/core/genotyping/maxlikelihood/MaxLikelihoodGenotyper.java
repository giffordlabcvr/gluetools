package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import uk.ac.gla.cvr.gluetools.core.genotyping.GenotypingResult;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.MaxLikelihoodGenotyperException.Code;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.TreeRenderer;
import uk.ac.gla.cvr.gluetools.core.treerenderer.TreeRendererContext;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.programs.mafft.add.MafftAddResult;
import uk.ac.gla.cvr.gluetools.programs.mafft.add.MafftAddRunner;
import uk.ac.gla.cvr.gluetools.programs.raxml.epa.RaxmlEpaResult;
import uk.ac.gla.cvr.gluetools.programs.raxml.epa.RaxmlEpaRunner;

@PluginClass(elemName="maxLikelihoodGenotyper")
public class MaxLikelihoodGenotyper extends ModulePlugin<MaxLikelihoodGenotyper> {

	public static final String ROOT_ALIGNMENT_NAME = "rootAlignmentName";
	public static final String PHYLO_MEMBER_WHERE_CLAUSE = "phyloMemberWhereClause";
	
	private static final String PHYLO_SUBTREE_ALIGNMENTS_KEY = "alignments";
	private static final String PHYLO_SUBTREE_MEMBER_KEY = "member";
	
	private String rootAlignmentName;
	private Optional<Expression> phyloMemberWhereClause;
	
	private MafftAddRunner mafftAddRunner = new MafftAddRunner();
	private RaxmlEpaRunner raxmlEpaRunner = new RaxmlEpaRunner();
	
	public MaxLikelihoodGenotyper() {
		super();
		addModulePluginCmdClass(GenotypeCommand.class);
		addSimplePropertyName(ROOT_ALIGNMENT_NAME);
		addSimplePropertyName(PHYLO_MEMBER_WHERE_CLAUSE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.rootAlignmentName = PluginUtils.configureStringProperty(configElem, ROOT_ALIGNMENT_NAME, true);
		this.phyloMemberWhereClause = 
				Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, PHYLO_MEMBER_WHERE_CLAUSE, false));
		
		Element mafftAddRunnerElem = PluginUtils.findConfigElement(configElem, "mafftAddRunner");
		if(mafftAddRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, mafftAddRunnerElem, mafftAddRunner);
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
			throw new MaxLikelihoodGenotyperException(Code.CONFIG_ERROR, "No such alignment \""+rootAlignmentName+"\"");
		}
		if(!rootAlignment.isConstrained()) {
			throw new MaxLikelihoodGenotyperException(Code.CONFIG_ERROR, "Alignment \""+rootAlignmentName+"\" is unconstrained");
		}
	}

	public List<GenotypingResult> genotype(CommandContext cmdContext, Map<String, DNASequence> querySequenceMap) {
		// could make some of these things configurable if necessary
		boolean recursive = true;
		boolean includeAllColumns = false;
		boolean deduplicate = true;
		String acRefName = null;
		String featureName = null;
		OrderStrategy orderStrategy = null;
		
		Alignment rootAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(rootAlignmentName));
		List<AlignmentMember> almtMembers = AlignmentListMemberCommand.listMembers(cmdContext, rootAlignment, recursive, deduplicate, phyloMemberWhereClause);
		Map<Map<String,String>, DNASequence> memberPkMapToAlignmentRow = 
				FastaAlignmentExporter.exportAlignment(cmdContext, 
						acRefName, featureName, includeAllColumns, orderStrategy, 
						rootAlignment, almtMembers);
		
		// we rename query and member sequences (a) to avoid clashes and (b) to work around program ID limitations.
		int rowNameIndex = 0;
		Map<String, Map<String,String>> rowNameToMemberPkMap = new LinkedHashMap<String, Map<String,String>>();
		Map<Map<String,String>, String> memberPkMapToRowName = new LinkedHashMap<Map<String,String>, String>();
		
		Map<String, DNASequence> almtFastaContent = new LinkedHashMap<String, DNASequence>();
		for(Map.Entry<Map<String,String>, DNASequence> entry: memberPkMapToAlignmentRow.entrySet()) {
			String rowName = "R"+rowNameIndex;
			rowNameToMemberPkMap.put(rowName, entry.getKey());
			memberPkMapToRowName.put(entry.getKey(), rowName);
			almtFastaContent.put(rowName, entry.getValue());
			rowNameIndex++;
		}
		
		Map<String, String> rowNameToQueryMap = new LinkedHashMap<String, String>();
		
		Map<String, DNASequence> queryFastaContent = new LinkedHashMap<String, DNASequence>();
		for(Map.Entry<String, DNASequence> entry: querySequenceMap.entrySet()) {
			String rowName = "Q"+rowNameIndex;
			rowNameToQueryMap.put(rowName, entry.getKey());
			queryFastaContent.put(rowName, entry.getValue());
			rowNameIndex++;
		}

		MafftAddResult maftAddResult = mafftAddRunner.executeMafftAdd(cmdContext, almtFastaContent, queryFastaContent);
		
		Map<String, DNASequence> alignmentWithQuery = maftAddResult.getAlignmentWithQuery();

		PhyloTree phyloTree = TreeRenderer.phyloTreeFromAlignment(cmdContext, rootAlignment, almtMembers, 
				new MaxLikelihoodTreeRendererContext(memberPkMapToRowName));
		
		RaxmlEpaResult raxmlEpaResult = raxmlEpaRunner.executeRaxmlEpa(cmdContext, phyloTree, alignmentWithQuery);

		
		return new ArrayList<GenotypingResult>();
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
			LinkedHashMap<String, Object> userData = new LinkedHashMap<String, Object>();
			List<String> alignmentNames = new ArrayList<String>();
			alignmentNames.add(alignment.getName());
			userData.put(PHYLO_SUBTREE_ALIGNMENTS_KEY, alignmentNames);
			phyloInternal.setUserData(userData);
			return phyloInternal;
		}

		@Override
		public PhyloLeaf phyloLeafForMember(CommandContext commandContext,
				AlignmentMember alignmentMember) {
			PhyloLeaf phyloLeaf = super.phyloLeafForMember(commandContext, alignmentMember);
			LinkedHashMap<String, Object> userData = new LinkedHashMap<String, Object>();
			List<String> alignmentNames = new ArrayList<String>();
			userData.put(PHYLO_SUBTREE_ALIGNMENTS_KEY, alignmentNames);
			userData.put(PHYLO_SUBTREE_MEMBER_KEY, alignmentMember.pkMap());
			phyloLeaf.setUserData(userData);
			return phyloLeaf;
		}

		// almtPhyloInternal is being optimized away, so we need to save any user data to singleBranchSubtree
		@Override
		public Map<String, Object> mergeUserData(
				PhyloInternal almtPhyloInternal,
				PhyloSubtree singleBranchSubtree) {
			Map<String, Object> subtreeUserData = singleBranchSubtree.getUserData();
			Map<String, Object> internalUserData = almtPhyloInternal.getUserData();
			if(subtreeUserData == null) {
				return internalUserData;
			}
			if(internalUserData == null) {
				return subtreeUserData;
			}
			List<String> subtreeAlmtNames = (List<String>) subtreeUserData.get(PHYLO_SUBTREE_ALIGNMENTS_KEY);
			List<String> internalAlmtNames = (List<String>) internalUserData.get(PHYLO_SUBTREE_ALIGNMENTS_KEY);
			subtreeAlmtNames.addAll(internalAlmtNames);
			return subtreeUserData;
		}

		@Override
		protected void setAlignmentPhyloInternalName(
				CommandContext commandContext, Alignment alignment,
				PhyloInternal alignmentPhyloInternal) {
			// no names required.
		}

		@Override
		protected void setMemberPhyloLeafName(CommandContext commandContext,
				AlignmentMember alignmentMember, PhyloLeaf memberPhyloLeaf) {
			memberPhyloLeaf.setName(memberPkMapToRowName.get(alignmentMember.pkMap()));
		}
		
		
		

	}
	
}
