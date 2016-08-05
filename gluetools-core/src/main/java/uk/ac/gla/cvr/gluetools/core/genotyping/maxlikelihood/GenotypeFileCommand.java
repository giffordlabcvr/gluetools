package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.io.File;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"genotype", "file"}, 
		description = "Genotype sequences in a file", 
		docoptUsages = { "-f <fileName> [-d <dataDir>]" },
		docoptOptions = { 
				"-f <fileName>, --fileName <fileName>  FASTA file path",
				"-d <dataDir>, --dataDir <dataDir>     Save algorithmic data in this directory",
		},
		furtherHelp = "If supplied, <dataDir> must either not exist or be an empty directory",
		metaTags = {CmdMeta.consoleOnly}	
)
public class GenotypeFileCommand extends AbstractGenotypeCommand {

	public final static String FILE_NAME = "fileName";
	public final static String DATA_DIR = "dataDir";
	
	private String fileName;
	private String dataDir;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILE_NAME, true);
		this.dataDir = PluginUtils.configureStringProperty(configElem, DATA_DIR, false);
	}
	
	@Override
	protected GenotypeCommandResult execute(CommandContext cmdContext, MaxLikelihoodGenotyper maxLikelihoodGenotyper) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		byte[] fastaBytes = consoleCommandContext.loadBytes(fileName);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> querySequenceMap = FastaUtils.parseFasta(fastaBytes);
		File dataDirFile = CommandUtils.ensureDataDir(cmdContext, dataDir);
		Map<String, GenotypeResult> genotypeResults = maxLikelihoodGenotyper.genotype(cmdContext, querySequenceMap, dataDirFile);
		return generateCommandResults(genotypeResults);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerPathLookup("dataDir", true);
		}
	}

	
}
