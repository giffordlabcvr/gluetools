package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class QueryCladeCategoryResult {

	@PojoDocumentField
	public String categoryName;

	@PojoDocumentField
	public String categoryDisplayName;

	@PojoDocumentListField(itemClass = QueryCladeResult.class)
	public List<QueryCladeResult> queryCladeResult = new ArrayList<QueryCladeResult>();
	
	@PojoDocumentField
	public String finalClade;

	@PojoDocumentField
	public String finalCladeRenderedName;

	// closest member of the final clade to the query.
	@PojoDocumentField
	public String closestMemberAlignmentName;
	
	@PojoDocumentField
	public String closestMemberSourceName;

	@PojoDocumentField
	public String closestMemberSequenceID;
}
