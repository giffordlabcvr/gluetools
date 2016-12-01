package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"export", "placement", "phylogeny"}, 
		description = "Export phylogeny file for a single placement in a result file", 
		docoptUsages = { "-i <inputFile> -q <queryName> -p <placementIndex> -o <outputFile> <outputFormat>" },
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>                 Placement results file",
				"-q <queryName>, --queryName <queryName>                 Query sequence name",
				"-p <placementIndex>, --placementIndex <placementIndex>  Placement index",
				"-o <outputFile>, --outputFile <outputFile>              Phylogeny output file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class ExportPlacementPhylogenyCommand extends AbstractPlacementCommand<OkResult> {

	public static final String OUTPUT_FILE = "outputFile";
	public static final String OUTPUT_FORMAT = "outputFormat";
	
	private String outputFile;
	private PhyloFormat outputFormat;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
		this.outputFormat = PluginUtils.configureEnumProperty(PhyloFormat.class, configElem, OUTPUT_FORMAT, true);
	}

	@Override
	protected OkResult executeOnPlacementResult(CommandContext cmdContext,
			MaxLikelihoodPlacer maxLikelihoodPlacer,
			MaxLikelihoodPlacerResult placerResult,
			MaxLikelihoodSingleQueryResult queryResult,
			MaxLikelihoodSinglePlacement placement) {
//		PhyloTree placementPhylogeny = 
//				maxLikelihoodPlacer.generatePlacementPhylogeny(placerResult, queryResult, placement);
//		((ConsoleCommandContext) cmdContext).saveBytes(outputFile, outputFormat.generate(placementPhylogeny));
		return new OkResult();
	}
	
	@CompleterClass
	public static class Completer extends AbstractPlacementCommandCompleter {
		public Completer() {
			super();
			registerPathLookup("outputFile", false);
			registerEnumLookup("outputFormat", PhyloFormat.class);
		}
		
	}


	
	
}
