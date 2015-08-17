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
