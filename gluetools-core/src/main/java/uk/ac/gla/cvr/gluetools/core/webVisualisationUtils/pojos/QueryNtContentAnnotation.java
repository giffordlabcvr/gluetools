package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class QueryNtContentAnnotation extends NtContentAnnotation {

	// locations (in "display space") where nucleotide differs from the reference.
	
	@PojoDocumentListField(itemClass = Integer.class)
	public List<Integer> ntDisplayPosDifferences = new ArrayList<Integer>();
}
