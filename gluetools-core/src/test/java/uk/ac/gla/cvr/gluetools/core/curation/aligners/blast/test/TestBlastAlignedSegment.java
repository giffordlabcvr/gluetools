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
package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHit;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastResult;

public class TestBlastAlignedSegment {

	
	@Test
	public void testHspSegments1() {
		//             [--]  [--]   [-]   [-]
		String qseq = "ACAGGGTTAGAAAGAG---GGG";
		String hseq = "ACTG--TTAG---GAGTTTGGG";
		int hitFrom = 50;
		int queryFrom = 20;

		List<String> actual = hspSegmentsAsStrings(qseq, hseq, hitFrom, queryFrom, 1, Function.identity());
		Assert.assertEquals(Arrays.asList(new String[]{
				"Ref: [50, 53] <-> Query: [20, 23]",
				"Ref: [54, 57] <-> Query: [26, 29]",
				"Ref: [58, 60] <-> Query: [33, 35]",
				"Ref: [64, 66] <-> Query: [36, 38]",
		}),actual);
	}

	// TBLASTN case.
	@Test
	public void testHspSegments2() {

		// AA alignment row
		
		// ----A---QRVP---
		
		// NT seq ref
		
		// A    Q    Z    R    V    P
		// ^    ^    ^    ^    ^    ^
		// 3000 3003 3006 3009 3012 3015
		
		String qseq = "AQ-RVP";
		String hseq = "AQZRVP";
		int hitFrom = 3000;
		int queryFrom = 1;

		List<String> actual = hspSegmentsAsStrings(qseq, hseq, hitFrom, queryFrom, 3, new Function<Integer, Integer>(){
			@Override
			public Integer apply(Integer t) {
				// alignment row query AA to NT mapping.
				if(t == 1) { return 13; }
				if(t == 2) { return 25; }
				if(t == 3) { return 28; }
				if(t == 4) { return 31; }
				if(t == 5) { return 34; }
				return null;
			}
			
		});
		Assert.assertEquals(Arrays.asList(new String[]{
				"Ref: [3000, 3002] <-> Query: [13, 15]", // A
				"Ref: [3003, 3005] <-> Query: [25, 27]", // Q
				"Ref: [3009, 3017] <-> Query: [28, 36]", // RVP
		}),actual);
	}

	public List<String> hspSegmentsAsStrings(
			String qseq, String hseq,
			int hitFrom, int queryFrom, int refIncrement, Function<Integer, Integer> queryCoordMapper) {
		BlastResult blastResult = new BlastResult();
		blastResult.setQueryFastaId("testQueryFastaId");
		BlastHit blastHit = new BlastHit(blastResult);
		blastHit.setReferenceName("testReferenceName");
		BlastHsp hsp = new BlastHsp(null);
		hsp.setHseq(hseq);
		hsp.setQseq(qseq);
		hsp.setHitFrom(hitFrom);
		hsp.setQueryFrom(queryFrom);
		List<BlastAlignedSegment> segments = hsp.computeBlastAlignedSegments(refIncrement, queryCoordMapper);
		List<String> resultsAsStrings = new ArrayList<String>();
		for(BlastAlignedSegment segment: segments) {
			resultsAsStrings.add(segment.toString());
		}
		return resultsAsStrings;
	}

	
}
