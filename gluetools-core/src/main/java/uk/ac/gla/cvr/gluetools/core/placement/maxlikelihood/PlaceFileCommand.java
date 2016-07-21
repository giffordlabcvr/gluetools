package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"place", "file"}, 
		description = "Place sequences from a file into a phylogeny", 
		docoptUsages = { "-f <fileName> [-d <dataDir>]" },
		docoptOptions = { 
				"-f <fileName>, --fileName <fileName>  FASTA file path",
				"-d <dataDir>, --dataDir <dataDir>     Save algorithmic data in this directory",
		},
		furtherHelp = "If supplied, <dataDir> must either not exist or be an empty directory",
		metaTags = {CmdMeta.consoleOnly}	
)
public class PlaceFileCommand extends AbstractPlaceCommand {

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
	protected PlaceCommandResult execute(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		File dataDirFile = null;
		if(dataDir != null) {
			dataDirFile = consoleCommandContext.fileStringToFile(dataDir);
			if(dataDirFile.exists()) {
				if(!dataDirFile.isDirectory()) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Not a directory: "+dataDirFile.getAbsolutePath());
				}
				if(dataDirFile.list().length > 0) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Not an empty directory: "+dataDirFile.getAbsolutePath());
				}
			} else {
				boolean mkdirsResult = dataDirFile.mkdirs();
				if(!mkdirsResult) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Failed to create directory: "+dataDirFile.getAbsolutePath());
				}
			}
		}
		
		byte[] fastaBytes = consoleCommandContext.loadBytes(fileName);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> querySequenceMap = FastaUtils.parseFasta(fastaBytes);
		Map<String, List<PlacementResult>> seqNameToPlacementResults = 
				maxLikelihoodPlacer.place(cmdContext, querySequenceMap, dataDirFile);
		return generatePlaceCommandResult(seqNameToPlacementResults);
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
