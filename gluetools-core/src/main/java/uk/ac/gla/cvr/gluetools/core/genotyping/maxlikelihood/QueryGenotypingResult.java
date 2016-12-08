package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class QueryGenotypingResult {

	@PojoDocumentField
	public String queryName;
	
	@PojoDocumentListField(itemClass = QueryCladeCategoryResult.class)
	public List<QueryCladeCategoryResult> queryCladeCategoryResult = new ArrayList<QueryCladeCategoryResult>();
	
	public QueryCladeCategoryResult getCladeCategoryResult(String categoryName) {
		return queryCladeCategoryResult.stream().filter(qccr -> qccr.categoryName.equals(categoryName)).findFirst().orElse(null);
	}
	
}
