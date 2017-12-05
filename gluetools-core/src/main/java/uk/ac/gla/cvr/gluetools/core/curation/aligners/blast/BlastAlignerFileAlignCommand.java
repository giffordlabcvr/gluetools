package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAligner.BlastAlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@CommandClass(
		commandWords = { Aligner.FILE_ALIGN_COMMAND_WORD }, 
		description = "Align sequence file to a reference using BLAST", 
		docoptUsages = { Aligner.FILE_ALIGN_COMMAND_DOCOPT_USAGE },
		metaTags = {  CmdMeta.consoleOnly },
		furtherHelp = Aligner.FILE_ALIGN_COMMAND_FURTHER_HELP
		)
public class BlastAlignerFileAlignCommand extends Aligner.FileAlignCommand<BlastAligner.BlastAlignerResult, BlastAligner> {

	@Override
	protected BlastAlignerResult execute(CommandContext cmdContext, BlastAligner modulePlugin) {
		return modulePlugin.computeConstrained(cmdContext, getReferenceName(), getQueryIdToNucleotides((ConsoleCommandContext) cmdContext));
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("referenceName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
			registerPathLookup("sequenceFileName", false);
		}
	}
	
}
