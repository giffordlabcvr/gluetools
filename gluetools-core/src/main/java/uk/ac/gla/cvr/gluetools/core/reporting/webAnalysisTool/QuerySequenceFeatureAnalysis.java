package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class QuerySequenceFeatureAnalysis extends SequenceFeatureAnalysis<QueryAa, QueryNtSegment> {

	@PojoDocumentListField(itemClass = VariationMatchGroup.class)
	public List<VariationMatchGroup> variationMatchGroupPresent = null;

	@PojoDocumentListField(itemClass = VariationMatchGroup.class)
	public List<VariationMatchGroup> variationMatchGroupAbsent = null;

}
