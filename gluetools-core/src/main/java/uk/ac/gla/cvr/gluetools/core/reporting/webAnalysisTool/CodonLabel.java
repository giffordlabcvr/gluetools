package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class CodonLabel {

	@PojoDocumentField
	public String label;

	@PojoDocumentField
	public Integer startUIndex;

	@PojoDocumentField
	public Integer endUIndex;
	
}
