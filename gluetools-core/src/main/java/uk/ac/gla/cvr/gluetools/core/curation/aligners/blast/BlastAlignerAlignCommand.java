package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAligner.BlastAlignerResult;

@CommandClass(
		commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
		description = "Align sequence data to a reference using BLAST", 
		docoptUsages = {}, 
		metaTags={  CmdMeta.inputIsComplex },
		furtherHelp = Aligner.ALIGN_COMMAND_FURTHER_HELP
		)
public class BlastAlignerAlignCommand extends Aligner.AlignCommand<BlastAligner.BlastAlignerResult, BlastAligner> {

	@Override
	protected BlastAlignerResult execute(CommandContext cmdContext, BlastAligner modulePlugin) {
		return modulePlugin.computeConstrained(cmdContext, getReferenceName(), getQueryIdToNucleotides());
	}
}