package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

public abstract class AaContentAnnotation extends VisualisationAnnotation {

	@PojoDocumentField 
	public Integer displayNtPos;

	@PojoDocumentField
	public String aa;
	
	@PojoDocumentField
	public Integer ntWidth;
	
	@PojoDocumentListField(itemClass = String.class)
	public List<String> multipleAas;
}
