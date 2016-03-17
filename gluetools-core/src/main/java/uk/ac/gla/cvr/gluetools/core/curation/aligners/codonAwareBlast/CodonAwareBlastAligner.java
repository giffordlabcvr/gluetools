package uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.AlignerException;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.AbstractBlastAligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast.CodonAwareBlastAligner.CodonAwareBlastAlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner.BlastType;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporaryMultiSeqBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

@PluginClass(elemName="codonAwareBlastAligner")
public class CodonAwareBlastAligner extends AbstractBlastAligner<CodonAwareBlastAlignerResult, CodonAwareBlastAligner> {


	public CodonAwareBlastAligner() {
		super();
		addModulePluginCmdClass(CodonAwareBlastAlignCommand.class);
		addModulePluginCmdClass(CodonAwareBlastFileAlignCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		if(getFeatureName() == null) {
			throw new AlignerException(AlignerException.Code.FEATURE_NAME_REQUIRED);
		}
	}




	@Override
	public CodonAwareBlastAlignerResult doAlign(CommandContext cmdContext,
			String refName, Map<String, DNASequence> queryIdToNucleotides) {
		String featureName = getFeatureName();
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(refName, featureName));
		
		List<LabeledAminoAcid> featureLocAminoAcids = FeatureLocAminoAcidCommand.featureLocAminoAcids(cmdContext, featureLoc);
		StringBuffer buf = new StringBuffer();
		TIntIntMap aaToNtMap = new TIntIntHashMap();
		int i = 1;
		for(LabeledAminoAcid laa: featureLocAminoAcids) {
			buf.append(laa.getAminoAcid());
			aaToNtMap.put(i, laa.getLabeledCodon().getNtStart());
			i++;
		}
		String queryAAs = buf.toString();

		String queryAAFastaID = refName+"_"+featureName;
		String queryAAFastaRow = FastaUtils.seqIdCompoundsPairToFasta(queryAAFastaID, queryAAs);
		byte[] queryAAFastaBytes = queryAAFastaRow.getBytes();

		BlastRunner blastRunner = getBlastRunner();
		BlastDbManager blastDbManager = BlastDbManager.getInstance();
		String uuid = UUID.randomUUID().toString();
		
		List<BlastResult> blastResults;
		try {
			TemporaryMultiSeqBlastDB multiSeqBlastDb = blastDbManager.createTempMultiSeqBlastDB(cmdContext, uuid, queryIdToNucleotides);
			blastResults = blastRunner.executeBlast(cmdContext, BlastType.TBLASTN, multiSeqBlastDb, queryAAFastaBytes);
		
		} finally {
			blastDbManager.removeTempMultiSeqBlastDB(cmdContext, uuid);
		}
		
		Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments = new LinkedHashMap<String, List<QueryAlignedSegment>>();
		Function<Integer, Integer> queryAAToNTCoordMapper = new Function<Integer, Integer>() {
			@Override
			public Integer apply(Integer t) {
				return aaToNtMap.get(t);
			}
		};
		for(String queryId: queryIdToNucleotides.keySet()) {
			Map<String, List<QueryAlignedSegment>> alignedSegsMap = 
					BlastUtils.tBlastNResultsToAlignedSegmentsMap(queryId, blastResults, 
							new MyBlastHspFilter(), queryAAToNTCoordMapper);
			
			List<QueryAlignedSegment> alignedSegs = alignedSegsMap.get(queryAAFastaID);
			
			fastaIdToAlignedSegments.put(queryId, alignedSegs.stream()
					.map(qas -> qas.invert())
					.collect(Collectors.toList()));
			
		}
		return new CodonAwareBlastAlignerResult(fastaIdToAlignedSegments);
		
	}

	
	
	
	
	
	
	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Aligner.AlignCommand> getAlignCommandClass() {
		return CodonAwareBlastAlignCommand.class;
	}
	
	public static class CodonAwareBlastAlignerResult extends Aligner.AlignerResult {
		public CodonAwareBlastAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("codonAwareBlastAlignerResult", fastaIdToAlignedSegments);
		}
	}

	@CommandClass(
			commandWords = { Aligner.ALIGN_COMMAND_WORD }, 
			description = "Align sequence data to a reference using codon-aware BLAST", 
			docoptUsages = {}, 
			metaTags={  CmdMeta.inputIsComplex },
			furtherHelp = Aligner.ALIGN_COMMAND_FURTHER_HELP
			)
	public static class CodonAwareBlastAlignCommand extends Aligner.AlignCommand<CodonAwareBlastAligner.CodonAwareBlastAlignerResult, CodonAwareBlastAligner> {

		@Override
		protected CodonAwareBlastAlignerResult execute(CommandContext cmdContext, CodonAwareBlastAligner modulePlugin) {
			return modulePlugin.doAlign(cmdContext, getReferenceName(), getQueryIdToNucleotides());
		}
	}

	@CommandClass(
			commandWords = { Aligner.FILE_ALIGN_COMMAND_WORD }, 
			description = "Align sequence file to a reference using codon-aware BLAST", 
			docoptUsages = { Aligner.FILE_ALIGN_COMMAND_DOCOPT_USAGE },
			metaTags = {  CmdMeta.consoleOnly },
			furtherHelp = Aligner.FILE_ALIGN_COMMAND_FURTHER_HELP
			)
	public static class CodonAwareBlastFileAlignCommand extends Aligner.FileAlignCommand<CodonAwareBlastAligner.CodonAwareBlastAlignerResult, CodonAwareBlastAligner> {

		@Override
		protected CodonAwareBlastAlignerResult execute(CommandContext cmdContext, CodonAwareBlastAligner modulePlugin) {
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

}
