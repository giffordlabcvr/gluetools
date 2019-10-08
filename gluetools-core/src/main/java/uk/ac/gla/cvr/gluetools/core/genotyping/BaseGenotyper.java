package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer.PlacerResultInternal;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodSingleQueryResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

public abstract class BaseGenotyper<T extends BaseGenotyper<T>> extends ModulePlugin<T> {

	public static final String MAX_LIKELIHOOD_PLACER_MODULE_NAME = "maxLikelihoodPlacerModuleName";
	private String maxLikelihoodPlacerModuleName;

	public abstract Map<String, QueryGenotypingResult> genotype(CommandContext cmdContext,
			PhyloTree glueProjectPhyloTree,
			Map<Integer, PhyloBranch> edgeIndexToPhyloBranch,
			Collection<MaxLikelihoodSingleQueryResult> singleQueryResults);
	
	protected BaseGenotyper() {
		addSimplePropertyName(MAX_LIKELIHOOD_PLACER_MODULE_NAME);
		registerModuleDocumentCmdClass(ListCladeCategoryCommand.class);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.maxLikelihoodPlacerModuleName = PluginUtils.configureStringProperty(configElem, MAX_LIKELIHOOD_PLACER_MODULE_NAME, true);
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		MaxLikelihoodPlacer maxLikelihoodPlacer = resolvePlacer(cmdContext);
		maxLikelihoodPlacer.validate(cmdContext);
	}

	
	public MaxLikelihoodPlacer resolvePlacer(CommandContext cmdContext) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = 
				Module.resolveModulePlugin(cmdContext, MaxLikelihoodPlacer.class, maxLikelihoodPlacerModuleName);
		return maxLikelihoodPlacer;
	}
	
	public Map<String, QueryGenotypingResult> genotype(CommandContext cmdContext, Map<String, DNASequence> querySequenceMap, File dataDirFile) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = resolvePlacer(cmdContext);
		PhyloTree glueProjectPhyloTree = maxLikelihoodPlacer.constructGlueProjectPhyloTree(cmdContext);
		PlacerResultInternal placerResult = maxLikelihoodPlacer.place(cmdContext, glueProjectPhyloTree, querySequenceMap, dataDirFile);
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = placerResult.getEdgeIndexToPhyloBranch();
		Collection<MaxLikelihoodSingleQueryResult> singleQueryResults = placerResult.getQueryResults().values();
		return genotype(cmdContext, glueProjectPhyloTree, edgeIndexToPhyloBranch, singleQueryResults);
	}

	// take the GLUE project phylo tree on which genotyping will be based and
	// map each reference member to the list of constrained ancestor alignments.
	protected Map<String, List<String>> referenceMemberAncestorAlmts(CommandContext cmdContext,
			PhyloTree glueProjectPhyloTree) {
		Map<String, List<String>> leafNameToAncestorAlmtNames = new LinkedHashMap<String, List<String>>();
		glueProjectPhyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafName = phyloLeaf.getName();
				Map<String,String> memberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, leafName);
				AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap);
				List<Alignment> ancestorAlmts = almtMember.getAlignment().getAncestors();
				List<String> ancestorAlmtNames = new ArrayList<String>();
				for(Alignment ancestorAlmt: ancestorAlmts) {
					ancestorAlmtNames.add(ancestorAlmt.getName());
				}
				leafNameToAncestorAlmtNames.put(leafName, ancestorAlmtNames);
			}
			
		});
		return leafNameToAncestorAlmtNames;
	}

	
	public abstract List<? extends BaseCladeCategory> getCladeCategories();

}