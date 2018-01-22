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

	@Test
	public void testIntersection9() {
		intersectionTest(
				refSegs(
						refSeg(915, 1490)
						),
				refSegs(
						refSeg(44, 197), 
						refSeg(198, 206), 
						refSeg(207, 844), 
						refSeg(845, 847), 
						refSeg(849, 874), 
						refSeg(1363, 1490), 
						refSeg(1674, 1763), 
						refSeg(1764, 1784), 
						refSeg(1785, 1787), 
						refSeg(1788, 1894), 
						refSeg(2634, 2765), 
						refSeg(2766, 2783), 
						refSeg(2784, 2821), 
						refSeg(3681, 3979), 
						refSeg(3982, 3985), 
						refSeg(3986, 3986), 
						refSeg(3987, 4059), 
						refSeg(7140, 7175), 
						refSeg(7176, 7275), 
						refSeg(8216, 8464), 
						refSeg(8574, 9080), 
						refSeg(9082, 9092), 
						refSeg(9093, 9373)
						),
				expectedIntersection(
						"Ref: [1363, 1490]"
						)
		);
	}
	
	@Test
	public void testIntersection10() {
		intersectionTest(
				refSegs(
						refSeg(4,8)
						),
				refSegs(
						refSeg(4,12)
						),
				expectedIntersection(
						"Ref: [4, 8]"
		));
	}

	@Test
	public void testIntersection11() {
		intersectionTest(
				refSegs(
						refSeg(4,8)
						),
				refSegs(
						refSeg(4,8)
						),
				expectedIntersection(
						"Ref: [4, 8]"
		));
	}

	@Test
	public void testIntersection12() {
		intersectionTest(
				refSegs(
						refSeg(4,12)
						),
				refSegs(
						refSeg(4,8)
						),
				expectedIntersection(
						"Ref: [4, 8]"
		));
	}

	@Test
	public void testCovers1() {
		coversTest(
				refSegs(
						refSeg(4,12)
						),
				refSegs(
						refSeg(4,8)
						),
				true
		);
	}

	@Test
	public void testCovers2() {
		coversTest(
				refSegs(
						refSeg(4,8)
						),
				refSegs(
						refSeg(4,12)
						),
				false
		);
	}

	@Test
	public void testCovers3() {
		coversTest(
				refSegs(
						refSeg(4,8),
						refSeg(9,12)
						),
				refSegs(
						refSeg(4,12)
						),
				true
		);
	}

	@Test
	public void testCovers4() {
		coversTest(
				refSegs(
						refSeg(4,12)
						),
				refSegs(
						refSeg(9,12)
						),
				true
		);
	}

	@Test
	public void testCovers5() {
		coversTest(
				refSegs(
						refSeg(1,2),
						refSeg(4,12)
						),
				refSegs(
						refSeg(9,12)
						),
				true
		);
	}

	@Test
	public void testCovers6() {
		coversTest(
				refSegs(
						refSeg(1,12)
						),
				refSegs(
						refSeg(4, 9)
						),
				true
		);
	}

	@Test
	public void testCovers7() {
		coversTest(
				refSegs(
						refSeg(1,12)
						),
				refSegs(
						refSeg(4, 13)
						),
				false
		);
	}

	
	@Test
	public void testCovers8() {
		coversTest(
				refSegs(
						refSeg(4, 9)
						),
				refSegs(
						refSeg(3, 9)
						),
				false
		);
	}

	
	@Test
	public void testCovers9() {
		coversTest(
				refSegs(
						refSeg(1, 3),
						refSeg(5, 9)
						),
				refSegs(
						refSeg(1, 3),
						refSeg(5, 9)
						),
				true
		);
	}

	@Test
	public void testCovers10() {
		coversTest(
				refSegs(
						refSeg(5, 9)
						),
				refSegs(
						refSeg(1, 3),
						refSeg(5, 9)
						),
				false
		);
	}

	
	@Test
	public void testSubtract1() {
		subtractTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(5,13)
						),
				expectedSubtractResult(
						"Ref: [4, 4]",
						"Ref: [14, 17]"
		));
	}

	@Test
	public void testSubtract2() {
		subtractTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(4,13)
						),
				expectedSubtractResult(
						"Ref: [14, 17]"
		));
	}

	@Test
	public void testSubtract3() {
		subtractTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(3,13)
						),
				expectedSubtractResult(
						"Ref: [14, 17]"
		));
	}

	
	@Test
	public void testSubtract4() {
		subtractTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(7,8)
						),
				expectedSubtractResult(
						"Ref: [4, 6]",
						"Ref: [12, 17]"
		));
	}

	
	@Test
	public void testSubtract5() {
		subtractTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(1,3),
						refSeg(7,8)
						),
				expectedSubtractResult(
						"Ref: [4, 6]",
						"Ref: [12, 17]"
		));
	}

	
	
	@Test
	public void testSubtract6() {
		subtractTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(14,15)
						),
				expectedSubtractResult(
						"Ref: [4, 8]",
						"Ref: [12, 13]",
						"Ref: [16, 17]"
		));
	}

	
	@Test
	public void testSubtract7() {
		subtractTest(
				refSegs(
						refSeg(4,8),
						refSeg(12,17)
						),
				refSegs(
						refSeg(2,8)
						),
				expectedSubtractResult(
						"Ref: [12, 17]"
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

	private static String[] expectedSubtractResult(String ... strings) {
		return strings;
	}

	
	private void intersectionTest(ReferenceSegment[] segs1, ReferenceSegment[] segs2, String[] expectedIntersection) {
		List<ReferenceSegment> intersection = 
				ReferenceSegment.intersection(Arrays.asList(segs1), Arrays.asList(segs2), new SegMerger());
		List<String> actualResults = intersection.stream().map(s -> s.toString()).collect(Collectors.toList());
		Assert.assertEquals(Arrays.asList(expectedIntersection), actualResults);
	}

	
	private void subtractTest(ReferenceSegment[] segs1, ReferenceSegment[] segs2, String[] expectedSubtractResult) {
		List<ReferenceSegment> subtractResult = 
				ReferenceSegment.subtract(Arrays.asList(segs1), Arrays.asList(segs2));
		List<String> actualResults = subtractResult.stream().map(s -> s.toString()).collect(Collectors.toList());
		Assert.assertEquals(Arrays.asList(expectedSubtractResult), actualResults);
	}

	
	private void coversTest(ReferenceSegment[] segs1, ReferenceSegment[] segs2, boolean expectedCoverResult) {
		boolean actualCoverResult = ReferenceSegment.covers(Arrays.asList(segs1), Arrays.asList(segs2));
		Assert.assertEquals(expectedCoverResult, actualCoverResult);
	}

	
	
	private class SegMerger implements BiFunction<ReferenceSegment, ReferenceSegment, ReferenceSegment> {
		@Override
		public ReferenceSegment apply(ReferenceSegment seg1, ReferenceSegment seg2) {
			return new ReferenceSegment(Math.max(seg1.getRefStart(), seg2.getRefStart()),
					Math.min(seg1.getRefEnd(), seg2.getRefEnd()));
		}
	}
	
}
