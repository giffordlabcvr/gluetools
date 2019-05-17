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
package uk.ac.gla.cvr.gluetools.programs.blast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastSegmentList;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class BlastUtils {

	public static Map<String, List<QueryAlignedSegment>> blastNResultsToAlignedSegmentsMap(String refName, 
			List<BlastResult> blastResults, BlastHspFilter blastHspFilter, boolean respectQueryOrder) {
		LinkedHashMap<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments = 
				new LinkedHashMap<String, List<QueryAlignedSegment>>();
		for(BlastResult blastResult: blastResults) {
			String queryFastaId = blastResult.getQueryFastaId();
			List<BlastHsp> hsps = blastResultToHsps(refName, blastHspFilter, blastResult);
			
			// generate segments from each HSP, and put all these together in a List.
			List<BlastSegmentList> perHspAlignedSegments = 
					hsps.stream()
					.map(hsp -> {
						checkBlastHsp(hsp);
						return hsp.computeBlastAlignedSegments(1, Function.identity());
					})
					.collect(Collectors.toList());
	
			// merge/rationalise the segments;
			BlastSegmentList mergedSegments = mergeSegments(perHspAlignedSegments, respectQueryOrder);
			// store merged segments against the query fasta ID.
			fastaIdToAlignedSegments.put(queryFastaId, new ArrayList<QueryAlignedSegment>(mergedSegments));
		}
		return fastaIdToAlignedSegments;
	}

	
	public static Map<String, List<QueryAlignedSegment>> tBlastNResultsToAlignedSegmentsMap(String refName, 
			List<BlastResult> blastResults, BlastHspFilter blastHspFilter, Function<Integer, Integer> queryAAToNTCoordMapper, boolean respectQueryOrder) {
		LinkedHashMap<String, List<QueryAlignedSegment>> fastaIdToAlignedSegments = 
				new LinkedHashMap<String, List<QueryAlignedSegment>>();
		for(BlastResult blastResult: blastResults) {
			String queryFastaId = blastResult.getQueryFastaId();
			List<BlastHsp> hsps = blastResultToHsps(refName, blastHspFilter, blastResult);
			
			// generate segments from each HSP, and put all these together in a List.
			List<BlastSegmentList> perHspAlignedSegments = 
					hsps.stream()
					.map(hsp -> {
						checkBlastHsp(hsp);
						return hsp.computeBlastAlignedSegments(3, queryAAToNTCoordMapper);
					})
					.collect(Collectors.toList());
	
			// merge/rationalise the segments;
			BlastSegmentList mergedSegments = mergeSegments(perHspAlignedSegments, respectQueryOrder);
			// store merged segments against the query fasta ID.
			fastaIdToAlignedSegments.put(queryFastaId, new ArrayList<QueryAlignedSegment>(mergedSegments));
		}
		return fastaIdToAlignedSegments;
	}

	
	
	
	public static List<BlastHsp> blastResultToHsps(String refName,
			BlastHspFilter blastHspFilter, BlastResult blastResult) {
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
		return hsps;
	}

	private static BlastSegmentList mergeSegments(
			List<BlastSegmentList> perHspAlignedSegments, boolean respectQueryOrder) {
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
			mergedSegments.mergeInSegmentList(nextSegments, respectQueryOrder);
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

}
