package uk.ac.gla.cvr.gluetools.programs.blast;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.curation.aligners.blast.BlastSegmentList;
import uk.ac.gla.cvr.gluetools.utils.SegmentUtils;

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

	public BlastSegmentList computeAlignedSegments() {
		BlastSegmentList segments = new BlastSegmentList();
		
		String hseq = getHseq();
		String qseq = getQseq();

		// hseq and qseq must be the same length
		int hqlength = hseq.length();
		
		// hqseqIndex: which position we are looking at on the hseq and qseq
		int hqseqIndex = 1;
		
		// range on the hit (reference) original sequence which the hsp covers
		int hitFrom = getHitFrom();
		// range on the query original sequence which the hsp covers
		int queryFrom = getQueryFrom();

		// refStart:refEnd, queryStart:queryEnd -- ranges of the next AlignedSegment which we will add.
		int refStart, refEnd;
		int queryStart, queryEnd;

		refStart = hitFrom;
		queryStart = queryFrom;
		
		refEnd = refStart;
		queryEnd = queryStart;

		char hNtChar;
		char qNtChar;

		hNtChar = SegmentUtils.ntChar(hseq, hqseqIndex);
		qNtChar = SegmentUtils.ntChar(qseq, hqseqIndex);
		while(hqseqIndex <= hqlength) {
			// read a block where neither hseq nor qseq has gaps
			while(hNtChar != '-' && qNtChar != '-' && hqseqIndex <= hqlength) {
				hqseqIndex++;
				refEnd++;
				queryEnd++;
				if(hqseqIndex <= hqlength) {
					hNtChar = SegmentUtils.ntChar(hseq, hqseqIndex);
					qNtChar = SegmentUtils.ntChar(qseq, hqseqIndex);
				}
			} 
			// at the end of this block add a new segment.
			BlastAlignedSegment blastAlignedSegment = 
					new BlastAlignedSegment(refStart, refEnd-1, queryStart, queryEnd-1, this);
			segments.add(blastAlignedSegment);
			// set coordinates for the next block
			refStart = refEnd;
			queryStart = queryEnd;
			// move over the hseq/qseq section as long as either side has gaps, 
			// incrementing refStart/queryStart appropriately
			while(  (hNtChar == '-' || qNtChar == '-') && 
					hqseqIndex <= hqlength ) {
				if(hNtChar != '-') {
					refStart++;
				}
				if(qNtChar != '-') {
					queryStart++;
				}
				hqseqIndex++;
				if(hqseqIndex <= hqlength) {
					hNtChar = SegmentUtils.ntChar(hseq, hqseqIndex);
					qNtChar = SegmentUtils.ntChar(qseq, hqseqIndex);
				}
			} 
			refEnd = refStart;
			queryEnd = queryStart;
		}
		return segments;
	}
	
	
}
