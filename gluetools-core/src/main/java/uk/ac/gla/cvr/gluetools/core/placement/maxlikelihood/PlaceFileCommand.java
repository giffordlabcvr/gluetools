package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer.PlacerResultInternal;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@CommandClass(
		commandWords={"place", "file"}, 
		description = "Place sequences from a file into a phylogeny", 
		docoptUsages = { "-i <inputFile> [-d <dataDir>] -o <outputFile>" },
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>     FASTA file path",
				"-o <outputFile>, --outputFile <outputFile>  Output file path for placement results",
				"-d <dataDir>, --dataDir <dataDir>           Save algorithmic data in this directory",
		},
		furtherHelp = "If supplied, <dataDir> must either not exist or be an empty directory",
		metaTags = {CmdMeta.consoleOnly}	
)
public class PlaceFileCommand extends AbstractPlaceCommand<OkResult> {

	public final static String INPUT_FILE = "inputFile";
	
	private String inputFile;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
	}

	@Override
	protected OkResult execute(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		byte[] fastaBytes = consoleCommandContext.loadBytes(inputFile);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> querySequenceMap = FastaUtils.parseFasta(fastaBytes);
		File dataDirFile = CommandUtils.ensureDataDir(consoleCommandContext, getDataDir());
		PlacerResultInternal placerResultInternal = maxLikelihoodPlacer.place(consoleCommandContext, querySequenceMap, dataDirFile);
		CommandDocument placerResultCmdDocument = PojoDocumentUtils.pojoToCommandDocument(placerResultInternal.toPojoResult());
		Document placerResultXmlDoc = CommandDocumentXmlUtils.commandDocumentToXmlDocument(placerResultCmdDocument);
		byte[] placerResultXmlBytes = GlueXmlUtils.prettyPrint(placerResultXmlDoc);
		consoleCommandContext.saveBytes(getOutputFile(), placerResultXmlBytes);
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AbstractPlaceCommandCompleter {
		public Completer() {
			super();
			registerPathLookup("inputFile", false);
		}
	}

	
}
