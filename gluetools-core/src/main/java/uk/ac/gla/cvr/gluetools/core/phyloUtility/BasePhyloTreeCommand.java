package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.DocumentToPhyloTreeTransformer;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class BasePhyloTreeCommand<R extends CommandResult> extends PhyloUtilityCommand<R> {

	private static final String INPUT_PHYLO_TREE = "inputPhyloTree";
	private CommandDocument inputPhyloTreeDocument;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputPhyloTreeDocument = PluginUtils.configureCommandDocumentProperty(configElem, INPUT_PHYLO_TREE, true);
	}

	
	@Override
	protected final R execute(CommandContext cmdContext, PhyloUtility phyloUtility) {
		DocumentToPhyloTreeTransformer documentToPhyloTreeTransformer = new DocumentToPhyloTreeTransformer();
		inputPhyloTreeDocument.accept(documentToPhyloTreeTransformer);
		PhyloTree phyloTree = documentToPhyloTreeTransformer.getPhyloTree();
		return execute(cmdContext, phyloUtility, phyloTree);
	}

	protected abstract R execute(CommandContext cmdContext, PhyloUtility phyloUtility, PhyloTree phyloTree);

}
