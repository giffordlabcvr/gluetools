package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

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
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.newick.NewickGenerator;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickGenerator;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.DocumentToPhyloTreeTransformer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.reporting.memberAnnotationGenerator.MemberAnnotationGenerator;

@CommandClass(
		commandWords={"tree-document-to-newick"}, 
		description = "Create a Newick string from a tree document", 
		docoptUsages = { }, 
		metaTags = {CmdMeta.inputIsComplex}	
)
public class TreeDocumentToNewickCommand extends ModulePluginCommand<TreeDocumentToNewickResult, TreeVisualiser> {

	public static final String TREE_DOCUMENT = "treeDocument";
	public static final String LEAF_TEXT_ANNOTATION_NAME = "leafTextAnnotationName";

	private CommandDocument treeDocument;
	private String leafTextAnnotationName;

	private final static String NEWICK_LEAF_NAME = "newickLeafName";
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.treeDocument = PluginUtils.configureCommandDocumentProperty(configElem, TREE_DOCUMENT, true);
		this.leafTextAnnotationName = PluginUtils.configureStringProperty(configElem, LEAF_TEXT_ANNOTATION_NAME, true);
	}

	@Override
	protected TreeDocumentToNewickResult execute(CommandContext cmdContext, TreeVisualiser treeVisualiser) {
		DocumentToPhyloTreeTransformer docToPhyloTreeTransformer = new DocumentToPhyloTreeTransformer();
		treeDocument.accept(docToPhyloTreeTransformer);
		
		PhyloTree phyloTree = docToPhyloTreeTransformer.getPhyloTree();
		
		setBranchAndTextProperties(cmdContext, treeVisualiser, phyloTree);
		
		PhyloTreeToNewickGenerator phyloTreeToNewickGenerator = new PhyloTreeToNewickGenerator(new NewickGenerator() {
			@Override
			public String generateLeafName(PhyloLeaf phyloLeaf) {
				return NewickGenerator.escapeNewickString((String) phyloLeaf.ensureUserData().get(NEWICK_LEAF_NAME));
			}
			
		});
		phyloTree.accept(phyloTreeToNewickGenerator);
		String newickString = phyloTreeToNewickGenerator.getNewickString();
		
		return new TreeDocumentToNewickResult(newickString);
	}

	private void setBranchAndTextProperties(CommandContext cmdContext, TreeVisualiser treeVisualiser, PhyloTree phyloTree) {
		List<MemberAnnotationGenerator> memberAnnotationGenerators = treeVisualiser.getMemberAnnotationGenerators();
		
		MemberAnnotationGenerator leafTextAnnotationGenerator = null;
		for(MemberAnnotationGenerator memberAnnotationGenerator: memberAnnotationGenerators) {
			if(memberAnnotationGenerator.getAnnotationName().equals(leafTextAnnotationName)) {
				leafTextAnnotationGenerator = memberAnnotationGenerator;
				break;
			}
		}
		if(leafTextAnnotationGenerator == null) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "TreeVisualiser module has no generator with annotationName = '"+leafTextAnnotationName+"'");
		}
		final MemberAnnotationGenerator leafTextAnnotationGeneratorFinal = leafTextAnnotationGenerator;
		
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				Map<String, Object> leafUserData = phyloLeaf.getUserData();
				String phyloLeafName = phyloLeaf.getName();
				boolean storedAlmtMember = true;
				if(leafUserData != null) {
					Object nonMemberValue = leafUserData.get("treevisualiser-nonmember");
					if(nonMemberValue != null && nonMemberValue.equals("true")) {
						storedAlmtMember = false;
					}
				} 
				String newickLeafName;
				if(storedAlmtMember) {
					Map<String, String> memberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, phyloLeafName);
					AlignmentMember member = GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap);
					newickLeafName = leafTextAnnotationGeneratorFinal.renderAnnotation(cmdContext, member);
				} else {
					newickLeafName = phyloLeafName;
				}
				leafUserData.put(NEWICK_LEAF_NAME, newickLeafName);
			}
			
		});
	}
}
