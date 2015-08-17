package uk.ac.gla.cvr.gluetools.programs.blast;

import java.util.Comparator;

public class BlastHspComparator implements Comparator<BlastHsp> {

	/**
	 * a negative int is returned when hsp1 is considered better, 
	 * a postive integer is returned when hsp2 is considered better.
	 */
	@Override
	public int compare(BlastHsp hsp1, BlastHsp hsp2) {
		// for bitScore, higher is better.
		int bitScoreResult = Double.compare(hsp1.getBitScore(), hsp2.getBitScore());
		if(bitScoreResult != 0) { 
			return bitScoreResult;
		}
		// for length, higher is better.
		int lengthResult = Integer.compare(hsp1.getAlignLen(), hsp2.getAlignLen());
		if(lengthResult != 0) { 
			return lengthResult;
		}
		// for gaps, lower is better.
		int gapsResult = -Integer.compare(hsp1.getAlignLen(), hsp2.getAlignLen());
		if(gapsResult != 0) { 
			return gapsResult;
		}
		
		// from now onwards, we're just making an arbitrary choice, but one which will give
		// stable / predictable behaviour.

		// prefer HSPs on the left of the reference
		int hitFromResult = Integer.compare(hsp1.getHitFrom(), hsp2.getHitFrom());
		if(hitFromResult != 0) { 
			return hitFromResult;
		}
		// prefer HSPs on the left of the query
		int queryFromResult = Integer.compare(hsp1.getQueryFrom(), hsp2.getQueryFrom());
		if(queryFromResult != 0) { 
			return queryFromResult;
		}
		return Integer.compare(
				(hsp1.getHseq()+hsp1.getQseq()).hashCode(), 
				(hsp2.getHseq()+hsp2.getQseq()).hashCode());
		
	}

}
