package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class DetailAnnotationRow extends VisualisationAnnotationRow<DetailAnnotation> {

	@PojoDocumentField
	public Integer trackNumber;
	
}
