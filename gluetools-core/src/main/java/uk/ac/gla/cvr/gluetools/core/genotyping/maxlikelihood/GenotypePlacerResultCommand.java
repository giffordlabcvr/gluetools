package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacerResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"genotype", "placer-result"}, 
		description = "Generate genotyping results from a placer result file", 
		docoptUsages = { "-f <fileName>" },
		docoptOptions = { 
				"-f <fileName>, --fileName <fileName>  Placer result file path",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class GenotypePlacerResultCommand extends AbstractGenotypeCommand {

	public final static String FILE_NAME = "fileName";
	
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
	}
	
	@Override
	protected GenotypeCommandResult execute(CommandContext cmdContext, MaxLikelihoodGenotyper maxLikelihoodGenotyper) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		byte[] placerResultBytes = consoleCommandContext.loadBytes(fileName);
		Document placerResultDocument = GlueXmlUtils.documentFromBytes(placerResultBytes);
		CommandDocument placerResultCmdDoc = CommandDocumentXmlUtils.xmlDocumentToCommandDocument(placerResultDocument);
		MaxLikelihoodPlacerResult placerResult = PojoDocumentUtils.commandObjectToPojo(placerResultCmdDoc, MaxLikelihoodPlacerResult.class);

		MaxLikelihoodPlacer placer = maxLikelihoodGenotyper.resolvePlacer(cmdContext);
		PhyloTree glueProjectPhyloTree = placer.constructGlueProjectPhyloTree(cmdContext);
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = 
				MaxLikelihoodPlacer.generateEdgeIndexToPhyloBranch(placerResult.getLabelledPhyloTree(), glueProjectPhyloTree);
		List<QueryGenotypingResult> genotypeResults = maxLikelihoodGenotyper.genotype(cmdContext, glueProjectPhyloTree, edgeIndexToPhyloBranch, placerResult.singleQueryResult);
		return new GenotypeCommandResult(maxLikelihoodGenotyper.getCladeCategories(), genotypeResults);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
	}

	
}
