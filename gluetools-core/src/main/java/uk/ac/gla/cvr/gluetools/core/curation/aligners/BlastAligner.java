package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@PluginClass(elemName="blastAligner")
public class BlastAligner extends Aligner<BlastAligner.BlastAlignerResult, BlastAligner> {

	
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		configure(pluginConfigContext, configElem);
	}
	
	
	@CommandClass(
			commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
			description = "Align sequence data to a reference using BLAST", 
			docoptUsages = { Aligner.ALIGN_COMMAND_DOCOPT_USAGE }, 
			docoptOptions = { Aligner.ALIGN_COMMAND_DOCOPT_OPTIONS }
	)
	public class BlastAlignCommand extends Aligner<BlastAligner.BlastAlignerResult, BlastAligner>.AlignCommand {

		@Override
		protected BlastAlignerResult execute(CommandContext cmdContext, BlastAligner modulePlugin) {
			// TODO Auto-generated method stub
			return null;
		}


	
	}

	public class BlastAlignerResult extends Aligner.AlignerResult {
		public BlastAlignerResult(String rootObjectName,
				List<AlignedSegment> alignedSegments) {
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

	@Override
	public Class<? extends Aligner<?, ?>.AlignCommand> getAlignCommandClass() {
		return BlastAlignCommand.class;
	}

	
	
}
