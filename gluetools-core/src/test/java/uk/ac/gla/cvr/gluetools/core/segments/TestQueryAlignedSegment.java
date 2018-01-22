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
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class TestQueryAlignedSegment {

	@Test
	public void testTranslate1() {
		translateTest(
				qaSegs( // S1 <-> R1a
						qaSeg(1, 20, 5, 24),
						qaSeg(29, 50, 25, 46)
						),
				qaSegs( // R1a <-> R1
						qaSeg(1, 47, 3, 49)
						),
				expectedTranslate(
						// S1 <-> R1
						"Ref: [7, 26] <-> Query: [1, 20]",
						"Ref: [27, 48] <-> Query: [29, 50]"
		));
	}

	
	@Test
	public void testTranslate2() {
		translateTest(
				qaSegs(
						// S1 <-> R1
						qaSeg(1, 20, 7, 26),
						qaSeg(29, 50, 27, 48)
						),
				qaSegs(
						// R1 <-> S2
						qaSeg(9, 24, 1, 15),
						qaSeg(30, 44, 17, 31)
						),
						// S2 <-> S1
				expectedTranslate(
						"Ref: [1, 16] <-> Query: [3, 18]",
						"Ref: [17, 31] <-> Query: [32, 46]"
		));
	}

	
	
	private static QueryAlignedSegment qaSeg(int memberStart, int memberEnd, int refStart, int refEnd) {
		return new QueryAlignedSegment(refStart, refEnd, memberStart, memberEnd);
	}
	
	private static QueryAlignedSegment[] qaSegs(QueryAlignedSegment ... qaSegs) {
		return qaSegs;
	}
	
	private static String[] expectedTranslate(String ... strings) {
		return strings;
	}
	
	private void translateTest(QueryAlignedSegment[] segs1, QueryAlignedSegment[] segs2, String[] expectedResultStrings) {
		List<QueryAlignedSegment> result = 
				QueryAlignedSegment.translateSegments(Arrays.asList(segs1), Arrays.asList(segs2));
		List<String> actualResultStrings = result.stream().map(s -> s.toString()).collect(Collectors.toList());
		Assert.assertEquals(Arrays.asList(expectedResultStrings), actualResultStrings);
	}

	

}
