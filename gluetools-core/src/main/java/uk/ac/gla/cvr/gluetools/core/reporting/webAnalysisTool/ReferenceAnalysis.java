package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

@PojoResultClass
public class ReferenceAnalysis {

	@PojoResultField
	public String refName;

	@PojoResultField
	public String sourceName;

	@PojoResultField
	public String sequenceID;
	
	@PojoResultField
	public String parentRefName;

	@PojoResultField
	public String containingAlmtName;

	@PojoResultField
	public List<SequenceFeatureAnalysis<ReferenceAa, ReferenceNtSegment>> sequenceFeatureAnalysis;
	
	private ReferenceSequence refSeq;
	private Alignment containingAlmt;
	private AlignmentMember containingAlmtMember;
	
	public ReferenceAnalysis(ReferenceSequence refSeq, Alignment containingAlmt, AlignmentMember containingAlmtMember) {
		this.refSeq = refSeq;
		this.refName = refSeq.getName();
		this.sourceName = refSeq.getSequence().getSource().getName();
		this.sequenceID = refSeq.getSequence().getSequenceID();
		this.parentRefName = containingAlmt == null ? null : containingAlmt.getRefSequence().getName();
		this.containingAlmtMember = containingAlmtMember;
		this.containingAlmt = containingAlmt;
		this.containingAlmtName = containingAlmt == null ? null : containingAlmt.getName();
	}

	public ReferenceSequence getRefSeq() {
		return refSeq;
	}

	public Alignment getContainingAlmt() {
		return containingAlmt;
	}

	public AlignmentMember getContainingAlmtMember() {
		return containingAlmtMember;
	}

	public Optional<SequenceFeatureAnalysis<ReferenceAa, ReferenceNtSegment>> getSeqFeatAnalysis(String featureName) {
		return sequenceFeatureAnalysis.stream().filter(seqFeatAnalysis -> seqFeatAnalysis.featureName.equals(featureName)).findFirst();
	}
}
