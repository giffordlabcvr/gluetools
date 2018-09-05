package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class VisualisationAnnotationRow<V extends VisualisationAnnotation> {

	@PojoDocumentField
	public String annotationType;
	
	@PojoDocumentListField(itemClass = VisualisationAnnotation.class)
	public List<V> annotations = new ArrayList<V>();
}
