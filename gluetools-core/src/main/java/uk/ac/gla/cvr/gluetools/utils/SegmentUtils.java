package uk.ac.gla.cvr.gluetools.utils;

public class SegmentUtils {

	public static class Segment {
		public int start,end;
		public Segment(int start, int end) {
			super();
			this.start = start;
			this.end = end;
		}
	}
	
	
	public static Segment overlap(Segment seg1, Segment seg2) {
		if(seg1.start > seg2.end || seg2.start > seg1.end) {
			return null; // no overlap
		}
		int overlapStart = Math.max(seg1.start, seg2.start);
		int overlapEnd = Math.min(seg1.end, seg2.end);
		return new Segment(overlapStart, overlapEnd);
	}
	
	
	public static String nt(String seq, int position) {
		return seq.substring(position-1, position);
	}

	public static char ntChar(String seq, int position) {
		return seq.charAt(position-1);
	}

	public static String subSeq(String seq, int start, int end) {
		if(start <= end) {
			return seq.substring(start-1, end);
		} else {
			return new StringBuilder(seq.substring(end-1, start)).reverse().toString();
		}
	}

	
}
