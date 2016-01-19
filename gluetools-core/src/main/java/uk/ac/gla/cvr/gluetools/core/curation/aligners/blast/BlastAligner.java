package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHspFilter;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.SingleReferenceBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="blastAligner")
public class BlastAligner extends Aligner<BlastAligner.BlastAlignerResult, BlastAligner> {

	private BlastRunner blastRunner = new BlastRunner();
	private Optional<Double> minimumBitScore;
	private Optional<Integer> minimumScore;
	private Boolean allowReverseHsps;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minimumBitScore = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, "minimumBitScore", false));
		minimumScore = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "minimumScore", false));
		allowReverseHsps = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "allowReverseHsps", false)).orElse(false);
		
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, "blastRunner");
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
		addProvidedCmdClass(ShowAlignerCommand.class);
		addProvidedCmdClass(ConfigureAlignerCommand.class);
		addProvidedCmdClass(BlastAlignCommand.class);
		addProvidedCmdClass(BlastFileAlignCommand.class);
	}
	
	
	@CommandClass(
			commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
			description = "Align sequence data to a reference using BLAST", 
			docoptUsages = {}, 
			metaTags={  CmdMeta.inputIsComplex },
			furtherHelp = Aligner.ALIGN_COMMAND_FURTHER_HELP
			)
	public static class BlastAlignCommand extends Aligner.AlignCommand<BlastAligner.BlastAlignerResult, BlastAligner> {

		@Override
		protected BlastAlignerResult execute(CommandContext cmdContext, BlastAligner modulePlugin) {
			return modulePlugin.doBlastAlign(cmdContext, getReferenceName(), getQueryIdToNucleotides());
		}
	}

	@CommandClass(
			commandWords = { Aligner.FILE_ALIGN_COMMAND_WORD }, 
			description = "Align sequence file to a reference using BLAST", 
			docoptUsages = { Aligner.FILE_ALIGN_COMMAND_DOCOPT_USAGE },
			metaTags = {  CmdMeta.consoleOnly },
			furtherHelp = Aligner.FILE_ALIGN_COMMAND_FURTHER_HELP
			)
	public static class BlastFileAlignCommand extends Aligner.FileAlignCommand<BlastAligner.BlastAlignerResult, BlastAligner> {

		@Override
		protected BlastAlignerResult execute(CommandContext cmdContext, BlastAligner modulePlugin) {
			return modulePlugin.doBlastAlign(cmdContext, getReferenceName(), getQueryIdToNucleotides((ConsoleCommandContext) cmdContext));
		}
	}

	
	public static class BlastAlignerResult extends Aligner.AlignerResult {
		public BlastAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("blastAlignerResult", fastaIdToAlignedSegments);
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

	public BlastAlignerResult doBlastAlign(CommandContext cmdContext, String refName, Map<String,DNASequence> queryIdToNucleotides) {
		byte[] fastaBytes = FastaUtils.mapToFasta(queryIdToNucleotides);
		List<BlastResult> blastResults;
		if(fastaBytes.length == 0) {
			blastResults = Collections.emptyList();
		} else {
			SingleReferenceBlastDB refDB = BlastDbManager.getInstance().ensureSingleReferenceDB(cmdContext, refName);
			blastResults = blastRunner.executeBlast(cmdContext, refDB, fastaBytes);
		}
		Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments = BlastUtils.blastNResultsToAlignedSegmentsMap(refName, blastResults, 
				new MyBlastHspFilter());
		return new BlastAlignerResult(fastaIdToAlignedSegments);
	}

	private class MyBlastHspFilter implements BlastHspFilter {

		@Override
		public boolean allowBlastHsp(BlastHsp blastHsp) {
			if(minimumBitScore.map(m -> blastHsp.getBitScore() < m).orElse(false)) {
				return false;
			}
			if(minimumScore.map(m -> blastHsp.getScore() < m).orElse(false)) {
				return false;
			}
			if(!allowReverseHsps && blastHsp.getQueryTo() < blastHsp.getQueryFrom()) {
				return false;
			}
			return true;
		}
		
	}
	
}
