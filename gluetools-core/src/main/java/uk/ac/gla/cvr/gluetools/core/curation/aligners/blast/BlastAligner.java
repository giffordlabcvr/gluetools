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
package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.Aligner;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.AlignerException;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.AlignerException.Code;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.SupportsComputeConstrained;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastRunner.BlastType;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastUtils;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.BlastDbManager;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.SingleReferenceBlastDB;
import uk.ac.gla.cvr.gluetools.programs.blast.dbManager.TemporarySingleSeqBlastDB;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@PluginClass(elemName="blastAligner", 
		description="Derives pairwise homologies using nucleotide BLAST")
public class BlastAligner extends AbstractBlastAligner<BlastAligner.BlastAlignerResult, BlastAligner> implements SupportsComputeConstrained {

	public BlastAligner() {
		super();
		registerModulePluginCmdClass(BlastAlignerAlignCommand.class);
		registerModulePluginCmdClass(BlastAlignerFileAlignCommand.class);
	}

	public static class BlastAlignerResult extends Aligner.AlignerResult {
		public BlastAlignerResult(Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments) {
			super("blastAlignerResult", fastaIdToAlignedSegments);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Aligner.AlignCommand> getComputeConstrainedCommandClass() {
		return BlastAlignerAlignCommand.class;
	}

	@Override
	public BlastAlignerResult computeConstrained(CommandContext cmdContext, String refName, Map<String,DNASequence> queryIdToNucleotides) {
		byte[] fastaBytes = FastaUtils.mapToFasta(queryIdToNucleotides, LineFeedStyle.LF);
		final Map<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments = initFastaIdToAlignedSegments(queryIdToNucleotides.keySet());
		
		// TODO cache reference DBs 
		// for specific feature locations (but this is only worth it for runs against non-coding features).
		if(!fastaIdToAlignedSegments.isEmpty()) {
			String featureName = getFeatureName();
			BlastDbManager blastDbManager = BlastDbManager.getInstance();
			if(featureName == null) {
				SingleReferenceBlastDB refDB = blastDbManager.ensureSingleReferenceDB(cmdContext, refName);
				List<BlastResult> blastResults = getBlastRunner().executeBlast(cmdContext, BlastType.BLASTN, refDB, fastaBytes);
				fastaIdToAlignedSegments.putAll(BlastUtils.blastNResultsToAlignedSegmentsMap(refName, blastResults, 
						new MyBlastHspFilter(), true));
			} else {
				FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(refName, featureName), true);
				if(featureLoc != null) {
					List<ReferenceSegment> refSegs = featureLoc.segmentsAsReferenceSegments();
					if(refSegs.size() == 0) {
						// do nothing
					} else if(refSegs.size() > 1) {
						throw new AlignerException(Code.CANNOT_ALIGN_AGAINST_DISCONTIGUOUS_FEATURE_LOCATION, refName, featureName);
					} else {
						ReferenceSegment refSeg = refSegs.get(0);
						CharSequence refNTs = featureLoc.getReferenceSequence().getSequence().getSequenceObject()
								.getNucleotides(cmdContext, refSeg.getRefStart(), refSeg.getRefEnd());
						String uuid = UUID.randomUUID().toString();
						String blastRefName = refName+":"+featureName;
						List<BlastResult> blastResults;
						try {
							TemporarySingleSeqBlastDB refDB = blastDbManager.createTempSingleSeqBlastDB(cmdContext, uuid, blastRefName, refNTs.toString());
							blastResults = getBlastRunner().executeBlast(cmdContext, BlastType.BLASTN, refDB, fastaBytes);
						} finally {
							blastDbManager.removeTempSingleSeqBlastDB(cmdContext, uuid);
						}
						fastaIdToAlignedSegments.putAll(BlastUtils.blastNResultsToAlignedSegmentsMap(blastRefName, blastResults, new MyBlastHspFilter(), true));
						int offset = refSeg.getRefStart()-1;
						fastaIdToAlignedSegments.forEach( (fastaId, alignedSegments) -> {
							alignedSegments.forEach(seg -> {
								seg.translateRef(offset);
							});
						});
					}
				}
			}
		}
		return new BlastAlignerResult(fastaIdToAlignedSegments);
	}
	
	
}
