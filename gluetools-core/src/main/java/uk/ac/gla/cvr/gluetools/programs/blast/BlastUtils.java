package uk.ac.gla.cvr.gluetools.programs.blast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastSegmentList;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class BlastUtils {

	public static Map<String, List<QueryAlignedSegment>> blastResultsToAlignedSegmentsMap(String refName, List<BlastResult> blastResults, 
			BlastHspFilter blastHspFilter) {
		LinkedHashMap<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments = new LinkedHashMap<String, List<QueryAlignedSegment>>();
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
			if(blastHspFilter != null) {
				// filter out non-allowed HSPs
				hsps = hsps.stream()
						.filter(blastHspFilter::allowBlastHsp)
						.collect(Collectors.toList());
			}
			
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
			fastaIdToAlignedSegments.put(queryFastaId, new ArrayList<QueryAlignedSegment>(mergedSegments));
		}
		return fastaIdToAlignedSegments;
	}

	private static BlastSegmentList mergeSegments(
			List<BlastSegmentList> perHspAlignedSegments) {
		if(perHspAlignedSegments.isEmpty()) {
			return new BlastSegmentList();
		}
		BlastSegmentList mergedSegments;
		// start with the segments from the highest scoring HSP
		BlastSegmentList highestScoringSegments = perHspAlignedSegments.remove(0);
		mergedSegments = IReferenceSegment.sortByRefStart(highestScoringSegments, BlastSegmentList::new);
		// fold in segments from other HSPs in descending score order.
		while(!perHspAlignedSegments.isEmpty()) {
			BlastSegmentList nextSegments = perHspAlignedSegments.remove(0);
			nextSegments = IReferenceSegment.sortByRefStart(nextSegments, BlastSegmentList::new);
			mergedSegments.mergeInSegmentList(nextSegments);
		}
		return mergedSegments;
	}

	// check the HSP for assumptions
	private static void checkBlastHsp(BlastHsp hsp) {
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

	private static void throwUnhandledException(String refName, String queryId, String message) {
		throw new BlastException(BlastException.Code.BLAST_UNHANDLED_CASE, refName, queryId, 
				message);
	}

	private static BlastSegmentList alignedSegmentsForHsp(BlastHsp hsp) {
		checkBlastHsp(hsp);
		return hsp.computeAlignedSegments();
	}

}
