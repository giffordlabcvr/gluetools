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

	private static final String ALLOW_REVERSE_HSPS = "allowReverseHsps";
	private static final String MINIMUM_SCORE = "minimumScore";
	private static final String MINIMUM_BIT_SCORE = "minimumBitScore";
	
	private BlastRunner blastRunner = new BlastRunner();
	private Optional<Double> minimumBitScore;
	private Optional<Integer> minimumScore;
	private Boolean allowReverseHsps;
	
	
	public BlastAligner() {
		super();
		addModulePluginCmdClass(BlastAlignCommand.class);
		addModulePluginCmdClass(BlastFileAlignCommand.class);
		addSimplePropertyName(ALLOW_REVERSE_HSPS);
		addSimplePropertyName(MINIMUM_BIT_SCORE);
		addSimplePropertyName(MINIMUM_SCORE);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		minimumBitScore = Optional.ofNullable(PluginUtils.configureDoubleProperty(configElem, MINIMUM_BIT_SCORE, false));
		minimumScore = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, MINIMUM_SCORE, false));
		allowReverseHsps = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_REVERSE_HSPS, false)).orElse(false);
		
		Element blastRunnerElem = PluginUtils.findConfigElement(configElem, "blastRunner");
		if(blastRunnerElem != null) {
			PluginFactory.configurePlugin(pluginConfigContext, blastRunnerElem, blastRunner);
		}
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
			return modulePlugin.doAlign(cmdContext, getReferenceName(), getQueryIdToNucleotides());
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
			return modulePlugin.doAlign(cmdContext, getReferenceName(), getQueryIdToNucleotides((ConsoleCommandContext) cmdContext));
		}
	}

	
	public static class BlastAlignerResult extends Aligner.AlignerResult {
		public BlastAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("blastAlignerResult", fastaIdToAlignedSegments);
		}

	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Aligner.AlignCommand> getAlignCommandClass() {
		return BlastAlignCommand.class;
	}

	@Override
	public BlastAlignerResult doAlign(CommandContext cmdContext, String refName, Map<String,DNASequence> queryIdToNucleotides) {
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
			if(!allowReverseHsps && 
					( (blastHsp.getQueryTo() < blastHsp.getQueryFrom()) ||
						(blastHsp.getHitTo() < blastHsp.getHitFrom())) ) {
				return false;
			}
			return true;
		}
		
	}
	
}
