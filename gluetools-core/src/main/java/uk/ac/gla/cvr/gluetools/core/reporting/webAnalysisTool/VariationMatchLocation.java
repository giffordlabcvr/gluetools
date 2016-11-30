package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class VariationMatchLocation {

	@PojoDocumentField
	public Integer startUIndex;

	@PojoDocumentField
	public Integer endUIndex;

}
