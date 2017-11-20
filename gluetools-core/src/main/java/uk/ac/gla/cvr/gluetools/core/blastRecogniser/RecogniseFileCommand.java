package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"recognise", "file"}, 
		description = "Apply recognition to sequences in a FASTA file", 
		docoptUsages = { "-i <inputFile>" },
		docCategory = "Type-specific module commands",
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>     FASTA file path",
		},
		metaTags = {CmdMeta.consoleOnly}	
)
public class RecogniseFileCommand extends ModulePluginCommand<BlastSequenceRecogniserResult, BlastSequenceRecogniser> 
	implements ProvidedProjectModeCommand {

	public final static String INPUT_FILE = "inputFile";
	
	private String inputFile;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.inputFile = PluginUtils.configureStringProperty(configElem, INPUT_FILE, true);
	}

	@Override
	protected BlastSequenceRecogniserResult execute(CommandContext cmdContext, BlastSequenceRecogniser blastSequenceRecogniser) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		byte[] fastaBytes = consoleCommandContext.loadBytes(inputFile);
		FastaUtils.normalizeFastaBytes(cmdContext, fastaBytes);
		Map<String, DNASequence> querySequenceMap = FastaUtils.parseFasta(fastaBytes);
		Map<String, List<RecognitionCategoryResult>> queryIdToCatResult = blastSequenceRecogniser.recognise(cmdContext, querySequenceMap);
		List<BlastSequenceRecogniserResultRow> resultRows = BlastSequenceRecogniserResultRow.rowsFromMap(queryIdToCatResult);
		return new BlastSequenceRecogniserResult(resultRows);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("inputFile", false);
		}
		
	}


}
