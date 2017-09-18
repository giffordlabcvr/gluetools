package uk.ac.gla.cvr.gluetools.core.fastaUtility;

import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@CommandClass(
		commandWords={"save-nucleotide-fasta"}, 
		description = "Save FASTA nucleotide command document to a file", 
		docoptUsages = {}, 
		metaTags = {CmdMeta.inputIsComplex, CmdMeta.consoleOnly}	
)
public class SaveNucleotideFastaCommand extends ModulePluginCommand<OkResult, FastaUtility>{

	public final static String FASTA_COMMAND_DOCUMENT = "fastaCommandDocument";
	private static final String OUTPUT_FILE = "outputFile";

	private String outputFile;
	private CommandDocument cmdDocument;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.cmdDocument = PluginUtils.configureCommandDocumentProperty(configElem, FASTA_COMMAND_DOCUMENT, true);
		this.outputFile = PluginUtils.configureStringProperty(configElem, OUTPUT_FILE, true);
	}

	
	@Override
	protected OkResult execute(CommandContext cmdContext, FastaUtility fastaUtility) {
		Map<String, DNASequence> querySequenceMap = FastaUtils.commandDocumentToNucleotideFastaMap(cmdDocument);
		byte[] fastaBytes = FastaUtils.mapToFasta(querySequenceMap, LineFeedStyle.forOS());
		((ConsoleCommandContext) cmdContext).saveBytes(outputFile, fastaBytes);
		return new OkResult();
	}

}
