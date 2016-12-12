package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;
import uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood.QueryCladeCategoryResult;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@PojoDocumentClass
public class QueryAnalysis {

	@PojoDocumentField
	public String fastaId;

	@PojoDocumentField
	public String targetRefName;

	@PojoDocumentField
	public String tipAlignmentName;
	
	@PojoDocumentListField(itemClass = String.class)
	public List<String> ancestorRefName;

	@PojoDocumentListField(itemClass = String.class)
	public List<String> ancestorAlmtName;

	@PojoDocumentListField(itemClass = ResultVariationCategory.class)
	public List<ResultVariationCategory> resultVariationCategory;
	
	@PojoDocumentListField(itemClass = QuerySequenceFeatureAnalysis.class)
	public List<QuerySequenceFeatureAnalysis> sequenceFeatureAnalysis;

	@PojoDocumentListField(itemClass = QueryCladeCategoryResult.class)
	public List<QueryCladeCategoryResult> queryCladeCategoryResult;
	
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
	
	public Optional<QuerySequenceFeatureAnalysis> getSeqFeatAnalysis(String featureName) {
		return sequenceFeatureAnalysis.stream().filter(seqFeatAnalysis -> seqFeatAnalysis.featureName.equals(featureName)).findFirst();
	}

	
}
