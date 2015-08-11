package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@PluginClass(elemName="blastAligner")
public class BlastAligner extends Aligner<BlastAligner.BlastAlignerResult, BlastAligner> {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		addProvidedCmdClass(ShowAlignerCommand.class);
		addProvidedCmdClass(ConfigureAlignerCommand.class);
		addProvidedCmdClass(BlastAlignCommand.class);
	}
	
	
	@CommandClass(
			commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
			description = "Align sequence data to a reference using BLAST", 
			docoptUsages = { Aligner.ALIGN_COMMAND_DOCOPT_USAGE }, 
			docoptOptions = { Aligner.ALIGN_COMMAND_DOCOPT_OPTIONS }
	)
	public static class BlastAlignCommand extends Aligner.AlignCommand<BlastAligner.BlastAlignerResult, BlastAligner> {

		@Override
		protected BlastAlignerResult execute(CommandContext cmdContext, BlastAligner modulePlugin) {
			String referenceNucleotides = getReferenceSequenceFormat().nucleotidesAsString(getReferenceSequenceBytes());
			String queryNucleotides = getQuerySequenceFormat().nucleotidesAsString(getQuerySequenceBytes());
			
			Random random = new java.util.Random();
			List<AlignedSegment> alignedSegments = new ArrayList<AlignedSegment>();
			{
				int seglength = random.nextInt(99)+1;
				int refStart = random.nextInt(referenceNucleotides.length()-seglength)+1;
				int refEnd = refStart + seglength - 1;
				int queryStart = random.nextInt(queryNucleotides.length()-seglength)+1;
				int queryEnd = queryStart + seglength - 1;
				alignedSegments.add(new AlignedSegment(refStart, refEnd, queryStart, queryEnd));
			}
			return new BlastAlignerResult(alignedSegments);
		}
	
	}

	public static class BlastAlignerResult extends Aligner.AlignerResult {
		public BlastAlignerResult(List<AlignedSegment> alignedSegments) {
			super("blastAlignerResult", alignedSegments);
		}

	}
	
	@CommandClass( 
			commandWords={"show", "configuration"}, 
			docoptUsages={},
			description="Show the current configuration of this aligner") 
	public static class ShowAlignerCommand extends ShowConfigCommand<BlastAligner> {}

	@SimpleConfigureCommandClass(
			propertyNames={}
	)
	public static class ConfigureAlignerCommand extends SimpleConfigureCommand<BlastAligner> {}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Aligner.AlignCommand> getAlignCommandClass() {
		return BlastAlignCommand.class;
	}

	
	
}
