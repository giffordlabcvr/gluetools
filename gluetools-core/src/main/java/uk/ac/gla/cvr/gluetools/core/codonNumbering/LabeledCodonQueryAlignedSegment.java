package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class LabeledCodonQueryAlignedSegment extends QueryAlignedSegment {

	private LabeledCodon labeledCodon;
	
	public LabeledCodonQueryAlignedSegment(LabeledCodon labeledCodon, int refStart, int refEnd, int queryStart, int queryEnd) {
		super(refStart, refEnd, queryStart, queryEnd);
		this.labeledCodon = labeledCodon;
	}

	public LabeledCodon getLabeledCodon() {
		return labeledCodon;
	}
	
	public LabeledCodonQueryAlignedSegment clone() {
		return new LabeledCodonQueryAlignedSegment(getLabeledCodon(), getRefStart(), getRefEnd(), getQueryStart(), getQueryEnd());
	}

}
