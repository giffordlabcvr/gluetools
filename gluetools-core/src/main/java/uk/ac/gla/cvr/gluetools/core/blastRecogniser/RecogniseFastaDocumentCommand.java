package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import java.util.List;
import java.util.Map;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@CommandClass(
		commandWords={"recognise", "fasta-document"}, 
		description = "Apply recognition to sequences in a FASTA document", 
		docoptUsages = { },
		docCategory = "Type-specific module commands",
		metaTags = {CmdMeta.inputIsComplex}	
)
public class RecogniseFastaDocumentCommand extends ModulePluginCommand<BlastSequenceRecogniserResult, BlastSequenceRecogniser> 
	implements ProvidedProjectModeCommand {

	public final static String FASTA_COMMAND_DOCUMENT = "fastaCommandDocument";
	
	private CommandDocument cmdDocument;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.cmdDocument = PluginUtils.configureCommandDocumentProperty(configElem, FASTA_COMMAND_DOCUMENT, true);
	}

	@Override
	protected BlastSequenceRecogniserResult execute(CommandContext cmdContext, BlastSequenceRecogniser blastSequenceRecogniser) {
		Map<String, DNASequence> querySequenceMap = FastaUtils.commandDocumentToNucleotideFastaMap(cmdDocument);
		Map<String, List<RecognitionCategoryResult>> queryIdToCatResult = blastSequenceRecogniser.recognise(cmdContext, querySequenceMap);
		List<BlastSequenceRecogniserResultRow> resultRows = BlastSequenceRecogniserResultRow.rowsFromMap(queryIdToCatResult);
		return new BlastSequenceRecogniserResult(resultRows);
	}
}
