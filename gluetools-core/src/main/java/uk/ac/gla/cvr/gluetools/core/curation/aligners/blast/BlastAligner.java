package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.AlignerException;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.AlignerException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDB;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.SingleReferenceBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="blastAligner")
public class BlastAligner extends AbstractBlastAligner<BlastAligner.BlastAlignerResult, BlastAligner> {

	public BlastAligner() {
		super();
		addModulePluginCmdClass(BlastAlignCommand.class);
		addModulePluginCmdClass(BlastFileAlignCommand.class);
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
		
		@CompleterClass
		public static class Completer extends AdvancedCmdCompleter {
			public Completer() {
				super();
				registerDataObjectNameLookup("referenceName", ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY);
				registerPathLookup("sequenceFileName", false);
			}
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
		Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments;
		
		if(fastaBytes.length == 0) {
			fastaIdToAlignedSegments = Collections.emptyMap();
		} else {
			String featureName = getFeatureName();
			BlastDbManager blastDbManager = BlastDbManager.getInstance();
			if(featureName == null) {
				SingleReferenceBlastDB refDB = blastDbManager.ensureSingleReferenceDB(cmdContext, refName);
				List<BlastResult> blastResults = getBlastRunner().executeBlast(cmdContext, refDB, fastaBytes);
				fastaIdToAlignedSegments = BlastUtils.blastNResultsToAlignedSegmentsMap(refName, blastResults, 
						new MyBlastHspFilter());
			} else {
				FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(refName, featureName));
				List<ReferenceSegment> refSegs = featureLoc.segmentsAsReferenceSegments();
				if(refSegs.size() == 0) {
					fastaIdToAlignedSegments = Collections.emptyMap();
				} else if(refSegs.size() > 1) {
					throw new AlignerException(Code.CANNOT_ALIGN_AGAINST_DISCONTIGUOUS_FEATURE_LOCATION, refName, featureName);
				}
				ReferenceSegment refSeg = refSegs.get(0);
				CharSequence refNTs = featureLoc.getReferenceSequence().getSequence().getSequenceObject()
					.getNucleotides(cmdContext, refSeg.getRefStart(), refSeg.getRefEnd());
				String uuid = UUID.randomUUID().toString();
				String blastRefName = refName+":"+featureName;
				List<BlastResult> blastResults;
				try {
					BlastDB refDB = blastDbManager.createTempSingleSeqBlastDB(cmdContext, uuid, blastRefName, refNTs.toString());
					blastResults = getBlastRunner().executeBlast(cmdContext, refDB, fastaBytes);
				} finally {
					blastDbManager.removeTempSingleSeqBlastDB(cmdContext, uuid);
				}
				fastaIdToAlignedSegments = BlastUtils.blastNResultsToAlignedSegmentsMap(blastRefName, blastResults, new MyBlastHspFilter());
				int offset = refSeg.getRefStart()-1;
				fastaIdToAlignedSegments.forEach( (fastaId, alignedSegments) -> {
					alignedSegments.forEach(seg -> {
						seg.translateRef(offset);
					});
				});
			}
			
		}
		return new BlastAlignerResult(fastaIdToAlignedSegments);
	}

	
}
