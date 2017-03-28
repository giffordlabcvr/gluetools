package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class PLocMatch {

	@PojoDocumentField
	public Integer pLocIndex;
	
	@PojoDocumentField
	public String matchedValue;
	
	@PojoDocumentField
	public Integer ntStart;
	
	@PojoDocumentField
	public Integer ntEnd;
	
	@PojoDocumentField
	public String lcStart;
	
	@PojoDocumentField
	public String lcEnd;
}
