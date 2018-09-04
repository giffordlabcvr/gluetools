package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;

@PojoDocumentClass
public class QueryAaContentAnnotation extends AaContentAnnotation {

	@PojoDocumentField
	public Boolean differentFromRef = false;
	
}
