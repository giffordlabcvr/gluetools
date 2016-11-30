package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class VariationMatch {

	@PojoDocumentField
	public String variationName;

	@PojoDocumentField
	public String variationRenderedName;
	
	@PojoDocumentField
	public Integer minStartUIndex;

	@PojoDocumentField
	public Integer maxEndUIndex;

	
	@PojoDocumentListField(itemClass = VariationMatchLocation.class)
	public List<VariationMatchLocation> locations = new ArrayList<VariationMatchLocation>();
	
	// display hint, to prevent overlapping variation hits, they are shown on separate "tracks". 
	// track number starts from 0.
	// non-null only if a "present" match
	@PojoDocumentField
	public Integer track;

}
