package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

		List<String> actual = hspSegmentsAsStrings(qseq, hseq, hitFrom, queryFrom);
		Assert.assertEquals(Arrays.asList(new String[]{
				"Ref: [50, 53] <-> Query: [20, 23]",
				"Ref: [54, 57] <-> Query: [26, 29]",
				"Ref: [58, 60] <-> Query: [33, 35]",
				"Ref: [64, 66] <-> Query: [36, 38]",
		}),actual);
	}

	public List<String> hspSegmentsAsStrings(
			String qseq, String hseq,
			int hitFrom, int queryFrom) {
		BlastResult blastResult = new BlastResult();
		blastResult.setQueryFastaId("testQueryFastaId");
		BlastHit blastHit = new BlastHit(blastResult);
		blastHit.setReferenceName("testReferenceName");
		BlastHsp hsp = new BlastHsp(null);
		hsp.setHseq(hseq);
		hsp.setQseq(qseq);
		hsp.setHitFrom(hitFrom);
		hsp.setQueryFrom(queryFrom);
		List<BlastAlignedSegment> segments = hsp.computeAlignedSegments();
		List<String> resultsAsStrings = new ArrayList<String>();
		for(BlastAlignedSegment segment: segments) {
			resultsAsStrings.add(segment.toString());
		}
		return resultsAsStrings;
	}

	
}
