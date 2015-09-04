package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class TestReferenceSegment {

	@Test
	public void testIntersection1() {
		intersectionTest(
				refSegs(
						refSeg(1,3)
						),
				refSegs(
						refSeg(3,5)
						),
				expectedIntersection(
						"Ref: [3, 3]"
		));
	}

	
	@Test
	public void testIntersection2() {
		intersectionTest(
				refSegs(
						refSeg(3,5)
						),
				refSegs(
						refSeg(1,4)
						),
				expectedIntersection(
						"Ref: [3, 4]"
		));
	}

	
	@Test
	public void testIntersection3() {
		intersectionTest(
				refSegs(
						refSeg(1,3),
						refSeg(5,7),
						refSeg(9,13),
						refSeg(16,20)
						),
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				expectedIntersection(
						"Ref: [5, 7]",
						"Ref: [12, 13]",
						"Ref: [16, 17]"
		));
	}

	@Test
	public void testIntersection4() {
		intersectionTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(1,3),
						refSeg(5,7),
						refSeg(9,13),
						refSeg(16,20)
						),
				expectedIntersection(
						"Ref: [5, 7]",
						"Ref: [12, 13]",
						"Ref: [16, 17]"
		));
	}
	

	@Test
	public void testIntersection5() {
		intersectionTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(9,11),
						refSeg(18,25)
						),
				expectedIntersection(
						));
	}

	@Test
	public void testIntersection6() {
		intersectionTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						),
				expectedIntersection(
						));
	}


	@Test
	public void testIntersection7() {
		intersectionTest(
				refSegs(
						),
				refSegs(
						refSeg(9,11),
						refSeg(18,25)
						),
				expectedIntersection(
						));
	}

	
	@Test
	public void testIntersection8() {
		intersectionTest(
				refSegs(
						),
				refSegs(
						),
				expectedIntersection(
						));
	}

	
	private static ReferenceSegment refSeg(int refStart, int refEnd) {
		return new ReferenceSegment(refStart, refEnd);
	}
	
	private static ReferenceSegment[] refSegs(ReferenceSegment ... refSegs) {
		return refSegs;
	}
	
	private static String[] expectedIntersection(String ... strings) {
		return strings;
	}
	
	private void intersectionTest(ReferenceSegment[] segs1, ReferenceSegment[] segs2, String[] expectedIntersection) {
		List<ReferenceSegment> intersection = 
				ReferenceSegment.intersection(Arrays.asList(segs1), Arrays.asList(segs2), new SegMerger());
		List<String> actualResults = intersection.stream().map(s -> s.toString()).collect(Collectors.toList());
		Assert.assertEquals(Arrays.asList(expectedIntersection), actualResults);
	}
	
	private class SegMerger implements BiFunction<ReferenceSegment, ReferenceSegment, ReferenceSegment> {
		@Override
		public ReferenceSegment apply(ReferenceSegment seg1, ReferenceSegment seg2) {
			return new ReferenceSegment(Math.max(seg1.getRefStart(), seg2.getRefStart()),
					Math.min(seg1.getRefEnd(), seg2.getRefEnd()));
		}
	}
	
}
