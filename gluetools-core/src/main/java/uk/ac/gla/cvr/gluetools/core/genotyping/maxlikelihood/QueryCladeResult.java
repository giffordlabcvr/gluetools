package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class QueryCladeResult {

	@PojoDocumentField
	public String cladeName;

	@PojoDocumentField
	public String cladeRenderedName;

	@PojoDocumentField
	public Double percentScore;
	
}
