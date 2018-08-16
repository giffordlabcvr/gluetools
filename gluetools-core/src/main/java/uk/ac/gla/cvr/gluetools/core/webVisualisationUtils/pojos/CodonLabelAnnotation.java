package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class CodonLabelAnnotation extends VisualisationAnnotation {

	@PojoDocumentField
	public String label;

	@PojoDocumentField
	public Integer ntWidth;
}
