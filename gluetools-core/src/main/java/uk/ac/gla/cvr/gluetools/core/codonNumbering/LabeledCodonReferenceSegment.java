package uk.ac.gla.cvr.gluetools.core.codonNumbering;

import java.util.function.BiFunction;

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class LabeledCodonReferenceSegment extends ReferenceSegment {

	private LabeledCodon labeledCodon;
	
	public LabeledCodonReferenceSegment(LabeledCodon labeledCodon, int refStart, int refEnd) {
		super(refStart, refEnd);
		this.labeledCodon = labeledCodon;
	}

	public LabeledCodon getLabeledCodon() {
		return labeledCodon;
	}
	
	public static BiFunction<LabeledCodonReferenceSegment, LabeledCodonReferenceSegment, LabeledCodonReferenceSegment> mergeAbuttingFunctionLabeledCodonReferenceSegment() {
		return (seg1, seg2) -> {
			return new LabeledCodonReferenceSegment(seg1.getLabeledCodon(), seg1.getRefStart(), seg2.getRefEnd());
		};
	}
	
	public LabeledCodonReferenceSegment clone() {
		return new LabeledCodonReferenceSegment(getLabeledCodon(), getRefStart(), getRefEnd());
	}
}
