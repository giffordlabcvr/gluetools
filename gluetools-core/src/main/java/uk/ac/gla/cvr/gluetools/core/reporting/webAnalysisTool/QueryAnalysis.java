package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@PojoResultClass
public class QueryAnalysis {

	@PojoResultField
	public String fastaId;

	@PojoResultField
	public String targetRefName;

	@PojoResultField
	public List<String> ancestorRefName;
	
	@PojoResultField
	public List<SequenceFeatureAnalysis<QueryAa, QueryNt>> sequenceFeatureAnalysis;

	private AbstractSequenceObject sequenceObj;

	private List<QueryAlignedSegment> queryToTargetRefSegs;
	
	public QueryAnalysis(String fastaId, AbstractSequenceObject sequenceObj, String targetRefName) {
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
	
	public Optional<SequenceFeatureAnalysis<QueryAa, QueryNt>> getSeqFeatAnalysis(String featureName) {
		return sequenceFeatureAnalysis.stream().filter(seqFeatAnalysis -> seqFeatAnalysis.featureName.equals(featureName)).findFirst();
	}

	
}