package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
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

	
	public final static String TREE_DOCUMENT = "treeDocument";
	public final static String PX_WIDTH = "pxWidth";
	public final static String PX_HEIGHT = "pxHeight";
	
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
		
		
		
		visDocument.setInt(PX_WIDTH, pxWidth);
		visDocument.setInt(PX_HEIGHT, pxHeight);
		return new VisualiseTreeResult(visDocument);
	}

}
