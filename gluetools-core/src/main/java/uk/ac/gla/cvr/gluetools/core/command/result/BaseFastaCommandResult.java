package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.Map;

import org.biojava.nbio.core.sequence.template.AbstractSequence;

import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public abstract class BaseFastaCommandResult extends CommandResult {

	protected BaseFastaCommandResult(CommandDocument commandDocument) {
		super(commandDocument);
	}

	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		CommandDocument commandDocument = getCommandDocument();
		Map<String, ? extends AbstractSequence<?>> fastaMap;
		String rootName = commandDocument.getRootName();
		if(rootName.equals(FastaUtils.NUCLEOTIDE_FASTA_DOC_ROOT)) {
			fastaMap = FastaUtils.commandDocumentToNucleotideFastaMap(commandDocument);
		} else if(rootName.equals(FastaUtils.AMINO_ACID_FASTA_DOC_ROOT)) {
			fastaMap = FastaUtils.commandDocumentToProteinFastaMap(commandDocument);
		} else {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Command document with root name "+rootName+" is not a FASTA document");
		}
		renderCtx.output(new String(FastaUtils.mapToFasta(fastaMap, LineFeedStyle.forOS())));
	}
	
}
