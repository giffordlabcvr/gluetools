package uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureLocAminoAcidCommand;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.AlignerException;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeConstrained;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.AbstractBlastAligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast.CodonAwareBlastAligner.CodonAwareBlastAlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
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
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

@PluginClass(elemName="codonAwareBlastAligner", 
		description="Derives pairwise homologies using codon-aware BLAST")
public class CodonAwareBlastAligner extends AbstractBlastAligner<CodonAwareBlastAlignerResult, CodonAwareBlastAligner> implements SupportsComputeConstrained {


	public CodonAwareBlastAligner() {
		super();
		addModulePluginCmdClass(CodonAwareBlastAlignerAlignCommand.class);
		addModulePluginCmdClass(CodonAwareBlastAlignerFileAlignCommand.class);
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
	public CodonAwareBlastAlignerResult computeConstrained(CommandContext cmdContext,
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
		String queryAAFastaRow = FastaUtils.seqIdCompoundsPairToFasta(queryAAFastaID, queryAAs, LineFeedStyle.forOS());
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
		
		final Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments = 
				initFastaIdToAlignedSegments(queryIdToNucleotides.keySet());
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
			if(alignedSegs == null) {
				alignedSegs = new ArrayList<QueryAlignedSegment>();
			}
			
			fastaIdToAlignedSegments.put(queryId, alignedSegs.stream()
					.map(qas -> qas.invert())
					.collect(Collectors.toList()));
			
		}
		return new CodonAwareBlastAlignerResult(fastaIdToAlignedSegments);
		
	}

	
	
	
	
	
	
	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Aligner.AlignCommand> getComputeConstrainedCommandClass() {
		return CodonAwareBlastAlignerAlignCommand.class;
	}
	
	public static class CodonAwareBlastAlignerResult extends Aligner.AlignerResult {
		public CodonAwareBlastAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("codonAwareBlastAlignerResult", fastaIdToAlignedSegments);
		}
	}


	
}
