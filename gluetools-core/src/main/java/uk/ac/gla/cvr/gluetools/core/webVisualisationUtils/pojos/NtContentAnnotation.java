package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

public abstract class NtContentAnnotation extends VisualisationAnnotation {

	@PojoDocumentField 
	public Integer displayNtPos;

	@PojoDocumentField
	public String ntContent;
}
