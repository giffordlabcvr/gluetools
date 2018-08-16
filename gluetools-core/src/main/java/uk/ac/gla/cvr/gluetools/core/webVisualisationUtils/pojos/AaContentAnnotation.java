package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

public abstract class AaContentAnnotation extends VisualisationAnnotation {

	@PojoDocumentField
	public String aa;
	
	@PojoDocumentField
	public Integer ntWidth;
}
