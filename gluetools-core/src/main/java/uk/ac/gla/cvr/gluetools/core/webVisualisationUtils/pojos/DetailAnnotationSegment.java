package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.segments.IReadOnlyReferenceSegment;

@PojoDocumentClass
public class DetailAnnotationSegment implements IReadOnlyReferenceSegment {

	@PojoDocumentField
	public String segmentId;
	
	@PojoDocumentField
	public Integer displayNtStart;

	@PojoDocumentField
	public Integer displayNtEnd;

	@Override
	public Integer getRefStart() {
		return displayNtStart;
	}

	@Override
	public Integer getRefEnd() {
		return displayNtEnd;
	}
	
}
