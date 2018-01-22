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

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastSegmentList;

public class TestBlastHsp extends TestBlast {

	@Test
	public void testRemoveNewOverlapsRef1() {
		
		BlastSegmentList existingSegments = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 31,        40,
				//       QueryStart QueryEnd
						 51,        60 ),
				//       RefStart   RefEnd
				segment( 46,        50,       
				//       QueryStart QueryEnd
						 61,        65 ));
		BlastSegmentList newSegments = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 26,        35,
				//       QueryStart QueryEnd
						 46,        55 ),
				//       RefStart   RefEnd
				segment( 51,        60,       
				//       QueryStart QueryEnd
						 71,        80 ));
		BlastSegmentList truncatedNewSegments = removeRefOverlaps(existingSegments, newSegments);
		Assert.assertEquals(new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 26,        30,
				//       QueryStart QueryEnd
						 46,        50 ),
				//       RefStart   RefEnd
				segment( 51,        60,       
				//       QueryStart QueryEnd
						 71,        80 )
		), truncatedNewSegments);
	}

	@Test
	public void testRemoveNewOverlapsRef2() {
			
			BlastSegmentList existingSegments = new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 21,        30,
					//       QueryStart QueryEnd
							 51,        60 ),
					//       RefStart   RefEnd
					segment( 46,        50,       
					//       QueryStart QueryEnd
							 61,        65 )
			);
			BlastSegmentList newSegments = new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 26,        35,
					//       QueryStart QueryEnd
							 46,        55 ),
					//       RefStart   RefEnd
					segment( 51,        60,       
					//       QueryStart QueryEnd
							 71,        80 )
			);
			BlastSegmentList truncatedNewSegments = removeRefOverlaps(existingSegments, newSegments);
			Assert.assertEquals(new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 31,        35,
					//       QueryStart QueryEnd
							 51,        55 ),
					//       RefStart   RefEnd
					segment( 51,        60,       
					//       QueryStart QueryEnd
							 71,        80 )
			), truncatedNewSegments);
	}
	
	@Test
	public void testRemoveNewOverlapsRef3() {
			
			BlastSegmentList existingSegments = new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 21,        30,
					//       QueryStart QueryEnd
							 51,        60 ),
					//       RefStart   RefEnd
					segment( 46,        50,       
					//       QueryStart QueryEnd
							 61,        65 )
			);
			BlastSegmentList newSegments = new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 31,        40,
					//       QueryStart QueryEnd
							 46,        55 ),
					//       RefStart   RefEnd
					segment( 51,        60,       
					//       QueryStart QueryEnd
							 71,        80 )
			);
			BlastSegmentList newSegmentsCopy = new BlastSegmentList(newSegments);
			BlastSegmentList truncatedNewSegments = removeRefOverlaps(existingSegments, newSegments);
			Assert.assertEquals(truncatedNewSegments, newSegmentsCopy);
	}

	@Test
	public void testRemoveNewOverlapsRef4() {
			
			BlastSegmentList existingSegments = new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 31,        40,
					//       QueryStart QueryEnd
							 46,        55 )
			);
			BlastSegmentList newSegments = new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 21,        50,
					//       QueryStart QueryEnd
							 51,        80 )
			);
			BlastSegmentList truncatedNewSegments = removeRefOverlaps(existingSegments, newSegments);
			Assert.assertEquals(truncatedNewSegments, 
					new BlastSegmentList(
							//       RefStart   RefEnd
							segment( 21,        30,
							//       QueryStart QueryEnd
									 51,        60 ),
							//       RefStart   RefEnd
							segment( 41,        50,
							//       QueryStart QueryEnd
									 71,        80 )
					)					
			);
	}

	
	@Test
	public void testRemoveNewOverlapsRef5() {
			
			BlastSegmentList existingSegments = new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 21,        50,
					//       QueryStart QueryEnd
							 51,        80 )
			);
			BlastSegmentList newSegments = new BlastSegmentList(
					//       RefStart   RefEnd
					segment( 31,        40,
					//       QueryStart QueryEnd
							 46,        55 )
			);
			BlastSegmentList truncatedNewSegments = removeRefOverlaps(existingSegments, newSegments);
			Assert.assertEquals(truncatedNewSegments, new BlastSegmentList());
	}

	
	
	public BlastSegmentList removeRefOverlaps(BlastSegmentList existingSegments, BlastSegmentList newSegments) {
		BlastSegmentList existingSegmentsCopy1 = new BlastSegmentList(existingSegments);
		BlastSegmentList existingSegmentsCopy2 = new BlastSegmentList();
		BlastSegmentList truncatedNewSegments = new BlastSegmentList();

		Function<BlastAlignedSegment, Integer> 
		getRefStart = BlastAlignedSegment::getRefStart,
		getRefEnd = BlastAlignedSegment::getRefEnd;
	
		BlastAlignedSegment.removeNewOverlaps(
				existingSegments, newSegments, 
				existingSegmentsCopy2, truncatedNewSegments, 
				getRefStart, getRefEnd);
		
		Assert.assertEquals(existingSegmentsCopy1, existingSegmentsCopy2);
		return truncatedNewSegments;
	}

	
}
