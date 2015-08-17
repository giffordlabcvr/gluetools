package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.test;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastSegmentList;

public class TestBlastSegmentList extends TestBlast {

	@Test
	public void testMergeInSegments() {
		BlastSegmentList existingSegments = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 31,        40,
				//       QueryStart QueryEnd
						 51,        60 ),

				//       RefStart   RefEnd
				segment( 46,        50,       
				//       QueryStart QueryEnd
						 61,        65 ));
		
		// merge in a list where the first segment overlaps on the reference.
		BlastSegmentList newSegments0 = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 26,        35,
				//       QueryStart QueryEnd
						 46,        55 ),
						 
				//       RefStart   RefEnd
				segment( 51,        60,       
				//       QueryStart QueryEnd
						 71,        80 ));
		existingSegments.mergeInSegmentList(newSegments0);
		
		// merge in a list where a segment overlaps on the query.
		BlastSegmentList newSegments1 = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 86,        95,       
				//       QueryStart QueryEnd
						 76,        85 ));
		existingSegments.mergeInSegmentList(newSegments1);

		// merge in a list which violates the implicit query sequence order.
		BlastSegmentList newSegments2 = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 41,        45,       
				//       QueryStart QueryEnd
						 141,        145 ));
		existingSegments.mergeInSegmentList(newSegments2);

		// merge in another list which violates the implicit query sequence order.
		BlastSegmentList newSegments3 = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 41,        45,       
				//       QueryStart QueryEnd
						 1,        5 ));
		existingSegments.mergeInSegmentList(newSegments3);

		// merge in another list which conforms to the implicit query sequence order.
		BlastSegmentList newSegments4 = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 101,       105,       
				//       QueryStart QueryEnd
						 101,       105 ));
		existingSegments.mergeInSegmentList(newSegments4);

		// merge in another list which conforms to the implicit query sequence order.
		BlastSegmentList newSegments5 = new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 96,        100,       
				//       QueryStart QueryEnd
						 86,        90 ));
		existingSegments.mergeInSegmentList(newSegments5);
		
		Assert.assertEquals(new BlastSegmentList(
				//       RefStart   RefEnd
				segment( 26,        30,
				//       QueryStart QueryEnd
						 46,        50 ),
						 
				//       RefStart   RefEnd
				segment( 31,        40,
				//       QueryStart QueryEnd
						 51,        60 ),
						 
				//       RefStart   RefEnd
				segment( 46,        50,       
				//       QueryStart QueryEnd
						 61,        65 ),
						 
				//       RefStart   RefEnd
				segment( 51,        60,       
				//       QueryStart QueryEnd
						 71,        80 ),
						 
				//       RefStart   RefEnd
				segment( 91,        95,       
				//       QueryStart QueryEnd
						 81,        85 ),

				segment( 96,        100,       
				//       QueryStart QueryEnd
			             86,        90 ),

				//       RefStart   RefEnd
				segment( 101,       105,       
				//       QueryStart QueryEnd
						 101,       105 )
		), existingSegments);

	}
	
}
