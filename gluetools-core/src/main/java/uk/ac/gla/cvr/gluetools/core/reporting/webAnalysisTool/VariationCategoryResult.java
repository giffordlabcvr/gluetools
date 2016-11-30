package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class VariationCategoryResult {

	@PojoDocumentField
	public String name;
	
	@PojoDocumentField
	public String displayName;
	
	@PojoDocumentField
	public Boolean reportAbsence;
	
}
