package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class TestQueryAlignedSegment {

	
	@Test
	public void testTranslateSegments() throws Exception {
		LinkedList<QueryAlignedSegment> queryToRef1Segments = new LinkedList<QueryAlignedSegment>(
				Arrays.asList(
						alignedSeg(11, 20, 21, 30),
						alignedSeg(30, 36, 35, 41),
						alignedSeg(45, 54, 55, 64),
						alignedSeg(60, 69, 70, 79),
						alignedSeg(75, 81, 80, 86)
						));
		LinkedList<QueryAlignedSegment> ref1ToRef2Segments = new LinkedList<QueryAlignedSegment>(Arrays.asList(
				alignedSeg(6, 15, 16, 25),
				alignedSeg(20, 26, 30, 36),
				alignedSeg(30, 39, 40, 49),
				alignedSeg(40, 45, 60, 65),
				alignedSeg(55, 65, 75, 85)
				));
		LinkedList<QueryAlignedSegment> translated = QueryAlignedSegment.translateSegments(queryToRef1Segments, ref1ToRef2Segments);
		Assert.assertEquals(Arrays.asList(
				"Ref: [6, 10] <-> Query: [26, 30]",
				"Ref: [20, 26] <-> Query: [35, 41]",
				"Ref: [35, 39] <-> Query: [55, 59]",
				"Ref: [40, 45] <-> Query: [70, 75]",
				"Ref: [55, 61] <-> Query: [80, 86]"
				), translated.stream()
					.map(QueryAlignedSegment::toString)
					.collect(Collectors.toList()));
	}
	
	
	private QueryAlignedSegment alignedSeg(int refStart, int refEnd, int queryStart, int queryEnd) {
		return new QueryAlignedSegment(refStart, refEnd, queryStart, queryEnd);
	}
}
