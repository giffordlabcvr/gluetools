package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class QueryAa extends Aa {

	@PojoDocumentListField(itemClass = String.class)
	public List<String> referenceDiffs = new ArrayList<String>();
	
}
