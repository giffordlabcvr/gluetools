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
package uk.ac.gla.cvr.gluetools.programs.blast;

import java.util.function.Function;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastSegmentList;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;

public class BlastHsp {

	private BlastHit blastHit;
	private double bitScore;
	private int score;
	private double evalue;
	private int identity;
	private int queryFrom;
	private int queryTo;
	private int hitFrom;
	private int hitTo;
	private Integer hitFrame;
	private int alignLen;
	private int gaps;
	private String qseq;
	private String hseq;
	
	public BlastHsp(BlastHit blastHit) {
		super();
		this.blastHit = blastHit;
	}
	public double getBitScore() {
		return bitScore;
	}
	public void setBitScore(double bitScore) {
		this.bitScore = bitScore;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public double getEvalue() {
		return evalue;
	}
	public void setEvalue(double evalue) {
		this.evalue = evalue;
	}
	public int getIdentity() {
		return identity;
	}

	public double getIdentityPct() {
		return (identity/ (double) alignLen)*100.0;
	}

	public void setIdentity(int identity) {
		this.identity = identity;
	}
	public int getQueryFrom() {
		return queryFrom;
	}
	public void setQueryFrom(int queryFrom) {
		this.queryFrom = queryFrom;
	}
	public int getQueryTo() {
		return queryTo;
	}
	public void setQueryTo(int queryTo) {
		this.queryTo = queryTo;
	}
	public int getHitFrom() {
		return hitFrom;
	}
	public void setHitFrom(int hitFrom) {
		this.hitFrom = hitFrom;
	}
	public int getHitTo() {
		return hitTo;
	}
	public void setHitTo(int hitTo) {
		this.hitTo = hitTo;
	}
	public int getAlignLen() {
		return alignLen;
	}
	public void setAlignLen(int alignLen) {
		this.alignLen = alignLen;
	}
	public int getGaps() {
		return gaps;
	}
	public void setGaps(int gaps) {
		this.gaps = gaps;
	}
	public String getQseq() {
		return qseq;
	}
	public void setQseq(String qseq) {
		this.qseq = qseq;
	}
	public String getHseq() {
		return hseq;
	}
	public void setHseq(String hseq) {
		this.hseq = hseq;
	}
	public BlastHit getBlastHit() {
		return blastHit;
	}
	public Integer getHitFrame() {
		return hitFrame;
	}
	public void setHitFrame(int hitFrame) {
		this.hitFrame = hitFrame;
	}
	public BlastSegmentList computeBlastAlignedSegments(int refIncrement, Function<Integer, Integer> queryCoordMapper) {
		BlastSegmentList segments = new BlastSegmentList();
		
		String hseq = getHseq();
		String qseq = getQseq();

		// hseq and qseq must be the same length
		int hqlength = hseq.length();
		
		// hqseqIndex: which position we are looking at on the hseq and qseq
		// (for tBlastN, the position in the amino acid)
		int hqseqIndex = 1;
		
		// range on the hit (reference) original sequence which the hsp covers
		int hitFrom = getHitFrom();
		int hitTo = getHitTo();
		// range on the query original sequence which the hsp covers
		int queryFrom = getQueryFrom();

		// refStart:refEnd, queryStart:queryEnd -- ranges of the next AlignedSegment which we will add.
		int refStart, refEnd;
		int queryStart, queryEnd;

		boolean forwardTranslation = ( getHitFrame() == null ) || ( getHitFrame() >= 0 ) ;
		
		if(forwardTranslation) {
	 		refStart = hitFrom;
			refEnd = refStart;
		} else {
			refEnd = hitTo+1;
	 		refStart = refEnd;
		}

		queryStart = queryFrom;
		queryEnd = queryStart;

		char hChar;
		char qChar;

		hChar = SegmentUtils.base1Char(hseq, hqseqIndex);
		qChar = SegmentUtils.base1Char(qseq, hqseqIndex);
		while(hqseqIndex <= hqlength) {
			Integer queryEndMapped = queryCoordMapper.apply(queryEnd);
			Integer lastQueryEndMapped;
			if(forwardTranslation) {
				lastQueryEndMapped = queryEndMapped-refIncrement;
			} else {
				lastQueryEndMapped = queryEndMapped+refIncrement;
			}
			// read a block where neither hseq nor qseq has gaps
			while(hChar != '-' && qChar != '-' && hqseqIndex <= hqlength && 
					( forwardTranslation ? 
					( queryEndMapped == lastQueryEndMapped+refIncrement ) : 
						(queryEndMapped == lastQueryEndMapped-refIncrement) ) ) {
				hqseqIndex++;
				if(forwardTranslation) {
					refEnd = refEnd + refIncrement;
				} else {
					refStart = refStart - refIncrement;
				}
				queryEnd++;
				lastQueryEndMapped = queryEndMapped;
				queryEndMapped = queryCoordMapper.apply(queryEnd);
				if(hqseqIndex <= hqlength) {
					hChar = SegmentUtils.base1Char(hseq, hqseqIndex);
					qChar = SegmentUtils.base1Char(qseq, hqseqIndex);
				}
			} 
			int segQStart, segQEnd;
			if(forwardTranslation) {
				segQStart = queryCoordMapper.apply(queryStart);
				segQEnd = (lastQueryEndMapped+refIncrement)-1;
			} else {
				segQStart = lastQueryEndMapped;
				segQEnd = (queryCoordMapper.apply(queryStart)+refIncrement)-1;
			}
			
			// at the end of this block add a new segment.
			BlastAlignedSegment blastAlignedSegment = 
					new BlastAlignedSegment(refStart, refEnd-1, segQStart, segQEnd, this);
			segments.add(blastAlignedSegment);
			// set coordinates for the next block
			if(forwardTranslation) {
				refStart = refEnd;
			} else {
				refEnd = refStart;
			}
			queryStart = queryEnd;
			// move over the hseq/qseq section as long as either side has gaps, 
			// incrementing refStart/queryStart appropriately
			while(  (hChar == '-' || qChar == '-') && 
					hqseqIndex <= hqlength ) {
				if(hChar != '-') {
					if(forwardTranslation) {
						refStart = refStart + refIncrement;
					} else {
						refEnd = refEnd - refIncrement;
					}
				}
				if(qChar != '-') {
					queryStart++;
				}
				hqseqIndex++;
				if(hqseqIndex <= hqlength) {
					hChar = SegmentUtils.base1Char(hseq, hqseqIndex);
					qChar = SegmentUtils.base1Char(qseq, hqseqIndex);
				}
			} 
			if(forwardTranslation) {
				refEnd = refStart;
			} else {
				refStart = refEnd;
			}
			queryEnd = queryStart;
		}
		return segments;
	}
	@Override
	public String toString() {
		return "BlastHsp [hitFrom=" + hitFrom + ", hitTo=" + hitTo + ", queryFrom=" + queryFrom + ", queryTo=" + queryTo
				+ ", hitFrame=" + hitFrame + ", bitScore=" + bitScore + ", identity=" + identity + ", evalue=" + evalue + ", score=" + score + "]";
	}
	
	
}
