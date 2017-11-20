package uk.ac.gla.cvr.gluetools.core.curation.aligners.compound;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.compound.CompoundAligner.CompoundAlignerResult;

@CommandClass(
		commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
		description = "Align sequence data to a reference", 
		docoptUsages = {}, 
		docCategory = "Type-specific module commands",
		metaTags={  CmdMeta.inputIsComplex },
		furtherHelp = Aligner.ALIGN_COMMAND_FURTHER_HELP
		)
public class CompoundAlignerAlignCommand extends Aligner.AlignCommand<CompoundAligner.CompoundAlignerResult, CompoundAligner> {

	@Override
	protected CompoundAlignerResult execute(CommandContext cmdContext, CompoundAligner modulePlugin) {
		return modulePlugin.computeConstrained(cmdContext, getReferenceName(), getQueryIdToNucleotides());
	}
}
