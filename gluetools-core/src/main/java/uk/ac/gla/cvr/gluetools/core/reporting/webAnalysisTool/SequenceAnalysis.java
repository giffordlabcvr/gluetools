package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@PojoResultClass
public class SequenceAnalysis {

	@PojoResultField
	public String fastaId;

	@PojoResultField
	public String targetRefName;

	@PojoResultField
	public List<String> ancestorRefName;
	
	@PojoResultField
	public List<NtAlignedSegment> ntAlignedSegment;

	@PojoResultField
	public List<SequenceFeatureAnalysis> sequenceFeatureAnalysis;

	private AbstractSequenceObject sequenceObj;

	private List<QueryAlignedSegment> queryToTargetRefSegs;
	
	public SequenceAnalysis(String fastaId, AbstractSequenceObject sequenceObj, String targetRefName) {
		this.fastaId = fastaId;
		this.targetRefName = targetRefName;
		this.sequenceObj = sequenceObj;
	}

	public AbstractSequenceObject getSequenceObj() {
		return sequenceObj;
	}

	public List<QueryAlignedSegment> getQueryToTargetRefSegs() {
		return queryToTargetRefSegs;
	}

	public void setQueryToTargetRefSegs(List<QueryAlignedSegment> queryToTargetRefSegs) {
		this.queryToTargetRefSegs = queryToTargetRefSegs;
	}
	
}
