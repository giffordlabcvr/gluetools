package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class VisualisationAnnotationRow {

	@PojoDocumentField
	public String annotationType;
	
	@PojoDocumentListField(itemClass = VisualisationAnnotation.class)
	public List<VisualisationAnnotation> annotations = new ArrayList<VisualisationAnnotation>();
}
