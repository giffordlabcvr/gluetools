package uk.ac.gla.cvr.gluetools.core.command.result;

import java.util.Map;

import org.biojava.nbio.core.sequence.template.AbstractSequence;

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
		Map<String, ? extends AbstractSequence<?>> ntFastaMap = FastaUtils.commandDocumentToNucleotideFastaMap(commandDocument);
		renderCtx.output(new String(FastaUtils.mapToFasta(ntFastaMap, LineFeedStyle.forOS())));
	}
	
}
