package uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast.CodonAwareBlastAligner.CodonAwareBlastAlignerResult;

@CommandClass(
		commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
		description = "Align sequence data to a reference using codon-aware BLAST", 
		docoptUsages = {}, 
		docCategory = "Type-specific module commands",
		metaTags={  CmdMeta.inputIsComplex },
		furtherHelp = Aligner.ALIGN_COMMAND_FURTHER_HELP
		)
public class CodonAwareBlastAlignerAlignCommand extends Aligner.AlignCommand<CodonAwareBlastAligner.CodonAwareBlastAlignerResult, CodonAwareBlastAligner> {

	@Override
	protected CodonAwareBlastAlignerResult execute(CommandContext cmdContext, CodonAwareBlastAligner modulePlugin) {
		return modulePlugin.computeConstrained(cmdContext, getReferenceName(), getQueryIdToNucleotides());
	}
}
