/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;import org.w3c.dom.Element;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.AlignerException;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.AlignerException.Code;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeConstrained;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.AbstractBlastAligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.codonAwareBlast.CodonAwareBlastAligner.CodonAwareBlastAlignerResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
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

	
	public static final String ALLOW_FEATURE_TO_BE_MISSING = "allowFeatureToBeMissing";

	// if true, feature may be missing on reference, in which case return an empty result.
	private boolean allowFeatureToBeMissing;
	
	public CodonAwareBlastAligner() {
		super();
		registerModulePluginCmdClass(CodonAwareBlastAlignerAlignCommand.class);
		registerModulePluginCmdClass(CodonAwareBlastAlignerFileAlignCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.allowFeatureToBeMissing = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, ALLOW_FEATURE_TO_BE_MISSING, false)).orElse(false);
		if(getFeatureName() == null) {
			throw new AlignerException(AlignerException.Code.FEATURE_NAME_REQUIRED);
		}
	}




	@Override
	public CodonAwareBlastAlignerResult computeConstrained(CommandContext cmdContext,
			String refName, Map<String, DNASequence> queryIdToNucleotides) {
		String featureName = getFeatureName();
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(refName, featureName), true);

		if(featureLoc == null) {
			if(allowFeatureToBeMissing) {
				return new CodonAwareBlastAlignerResult(Collections.emptyMap());
			} else {
				throw new AlignerException(Code.MISSING_FEATURE_LOCATION, refName, featureName);
			}
		}

		featureLoc.getFeature().checkCodesAminoAcids();

		List<LabeledQueryAminoAcid> featureLocAminoAcids = featureLoc.getReferenceAminoAcidContent(cmdContext);
		StringBuffer buf = new StringBuffer();
		TIntIntMap aaToNtMap = new TIntIntHashMap();
		int i = 1;
		for(LabeledQueryAminoAcid lqaa: featureLocAminoAcids) {
			buf.append(lqaa.getLabeledAminoAcid().getAminoAcid());
			aaToNtMap.put(i, lqaa.getLabeledAminoAcid().getLabeledCodon().getNtStart());
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
							new MyBlastHspFilter(), queryAAToNTCoordMapper, true);
			
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
