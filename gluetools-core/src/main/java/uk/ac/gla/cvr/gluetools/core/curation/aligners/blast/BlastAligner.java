package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowConfigCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.SimpleConfigureCommandClass;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner.AlignerResult.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAlignerException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHit;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHspComparator;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;

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
	}
	
	
	@CommandClass(
			commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
			description = "Align sequence data to a reference using BLAST", 
			docoptUsages = { Aligner.ALIGN_COMMAND_DOCOPT_USAGE }, 
			docoptOptions = { Aligner.ALIGN_COMMAND_DOCOPT_OPTION1, Aligner.ALIGN_COMMAND_DOCOPT_OPTION2 }
	)
	public static class BlastAlignCommand extends Aligner.AlignCommand<BlastAligner.BlastAlignerResult, BlastAligner> {

		@Override
		protected BlastAlignerResult execute(CommandContext cmdContext, BlastAligner modulePlugin) {
			String refName = getReferenceName();
			String queryFasta = getQueryFasta();
			return modulePlugin.doBlastAlign(cmdContext, refName, queryFasta);
		}

	
	}

	public static class BlastAlignerResult extends Aligner.AlignerResult {
		public BlastAlignerResult(Map<String, List<AlignedSegment>> fastaIdToAlignedSegments) {
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

	public BlastAlignerResult doBlastAlign(CommandContext cmdContext, String refName, String queryFasta) {
		List<BlastResult> blastResults = blastRunner.executeBlast(cmdContext, refName, queryFasta);
		Map<String, List<AlignedSegment>> fastaIdToAlignedSegments = blastResultsToAlignedSegmentsMap(refName, blastResults);
		return new BlastAlignerResult(fastaIdToAlignedSegments);
	}

	public Map<String, List<AlignedSegment>> blastResultsToAlignedSegmentsMap(String refName, List<BlastResult> blastResults) {
		LinkedHashMap<String, List<AlignedSegment>> fastaIdToAlignedSegments = new LinkedHashMap<String, List<AlignedSegment>>();
		for(BlastResult blastResult: blastResults) {
			String queryFastaId = blastResult.getQueryFastaId();
			// find hits on the specified reference
			List<BlastHit> hits =
					blastResult.getHits().stream()
					.filter(hit -> hit.getReferenceName().equals(refName))
					.collect(Collectors.toList());
			// merge all hit HSPs together
			List<BlastHsp> hsps = hits.stream()
					.map(BlastHit::getHsps)
					.flatMap(hspList -> hspList.stream())
					.collect(Collectors.toList());
			// filter out non-allowed HSPs
			hsps = hsps.stream()
					.filter(this::allowedHsp)
					.collect(Collectors.toList());
			
			// sort HSPs according to our comparator.
			Collections.sort(hsps, new BlastHspComparator());
			
			// generate segments from each HSP, and put all these together in a List.
			List<BlastSegmentList> perHspAlignedSegments = 
					hsps.stream()
					.map(hsp -> alignedSegmentsForHsp(hsp))
					.collect(Collectors.toList());

			// merge/rationalise the segments;
			BlastSegmentList mergedSegments = mergeSegments(perHspAlignedSegments);
			// store merged segments against the query fasta ID.
			fastaIdToAlignedSegments.put(queryFastaId, new ArrayList<AlignedSegment>(mergedSegments));
		}
		return fastaIdToAlignedSegments;
	}
	
	private BlastSegmentList mergeSegments(
			List<BlastSegmentList> perHspAlignedSegments) {
		if(perHspAlignedSegments.isEmpty()) {
			return new BlastSegmentList();
		}
		BlastSegmentList mergedSegments;
		// start with the segments from the highest scoring HSP
		mergedSegments = perHspAlignedSegments.remove(0);
		// fold in segments from other HSPs in descending score order.
		while(!perHspAlignedSegments.isEmpty()) {
			mergedSegments.mergeInSegmentList(perHspAlignedSegments.remove(0));
		}
		return mergedSegments;
	}

	
	


	// check the HSP for assumptions
	private void checkBlastHsp(BlastHsp hsp) {
		String refName = hsp.getBlastHit().getReferenceName();
		String queryId = hsp.getBlastHit().getBlastResult().getQueryFastaId();

		String hseq = hsp.getHseq();
		String qseq = hsp.getQseq();
		
		int hqlength = hseq.length();
		if(hqlength != qseq.length()) {
			throwUnhandledException(refName, queryId, "hseq and qseq are different lengths"); 
		}
		if(hseq.startsWith("-")) {
			throwUnhandledException(refName, queryId, "hseq starts with a gap"); 
		}
		if(qseq.startsWith("-")) {
			throwUnhandledException(refName, queryId, "qseq starts with a gap"); 
		}
		if(hseq.endsWith("-")) {
			throwUnhandledException(refName, queryId, "hseq ends with a gap"); 
		}
		if(qseq.endsWith("-")) {
			String string = "qseq ends with a gap";
			throwUnhandledException(refName, queryId, string); 
		}

	}

	public void throwUnhandledException(String refName, String queryId,
			String message) {
		throw new BlastAlignerException(Code.BLAST_ALIGNER_UNHANDLED_CASE, refName, queryId, 
				message);
	}
	

	private BlastSegmentList alignedSegmentsForHsp(BlastHsp hsp) {
		checkBlastHsp(hsp);
		return hsp.computeAlignedSegments();
	}
	
	private boolean allowedHsp(BlastHsp hsp) {
		if(minimumBitScore.map(m -> hsp.getBitScore() < m).orElse(false)) {
			return false;
		}
		if(minimumScore.map(m -> hsp.getScore() < m).orElse(false)) {
			return false;
		}
		if(!allowReverseHsps && hsp.getQueryTo() < hsp.getQueryFrom()) {
			return false;
		}
		return true;
	}
}
