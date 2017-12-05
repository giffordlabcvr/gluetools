package uk.ac.gla.cvr.gluetools.core.treerenderer;

import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporterException.Code;

@PluginClass(elemName="phyloExporter",
		description="Exports a phylogenetic tree stored as auxiliary data in Alignment tree nodes")
public class PhyloExporter extends ModulePlugin<PhyloExporter> {

	public PhyloExporter() {
		super();
		registerModulePluginCmdClass(ExportPhylogenyCommand.class);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}

	public static PhyloTree exportAlignmentPhyloTree(CommandContext cmdContext, Alignment alignment, String fieldName, Boolean recursive) {
		String phyloTreeString = (String) alignment.readProperty(fieldName);
		PhyloFormat phyloFormat = Alignment.getPhylogenyPhyloFormat(cmdContext);
		PhyloTree localPhyloTree = phyloFormat.parse(phyloTreeString.getBytes());
		if(recursive) {
			localPhyloTree.accept(new PhyloTreeVisitor() {
				@Override
				public void visitLeaf(PhyloLeaf phyloLeaf) {
					String leafName = phyloLeaf.getName();
					if(Project.validTargetPath(ConfigurableTable.alignment.getModePath(), leafName)) {
						Map<String, String> alignmentPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment, leafName);
						Alignment childAlignment = GlueDataObject.lookup(cmdContext, Alignment.class, alignmentPkMap);
						if(!childAlignment.getParent().getName().equals(alignment.getName())) {
							throw new PhyloExporterException(Code.PHYLOGENY_REFERENCES_NON_CHILD_ALIGNMENT, alignment.getName(), fieldName, childAlignment.getName());
						}
						PhyloTree childPhyloTree = exportAlignmentPhyloTree(cmdContext, childAlignment, fieldName, true);
						PhyloSubtree<?> childRootSubtree = childPhyloTree.getRoot();
						childPhyloTree.setRoot(null);
						PhyloBranch parentPhyloBranch = phyloLeaf.getParentPhyloBranch();
						if(parentPhyloBranch != null) {
							parentPhyloBranch.setSubtree(childRootSubtree);
						} else {
							localPhyloTree.setRoot(childRootSubtree);
						}
					}
				}
			});
		}
		return localPhyloTree;
	}
	
}
