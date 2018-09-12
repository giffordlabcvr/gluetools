package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;
import uk.ac.gla.cvr.gluetools.core.segments.IReadOnlyReferenceSegment;

@PojoDocumentClass
public class DetailAnnotation extends VisualisationAnnotation implements IReadOnlyReferenceSegment {

	@PojoDocumentField
	public String detailId;

	@PojoDocumentListField(itemClass = DetailAnnotationSegment.class)
	public List<DetailAnnotationSegment> segments = new ArrayList<DetailAnnotationSegment>();

	@PojoDocumentField
	public Integer minRefStart;

	@PojoDocumentField
	public Integer maxRefEnd;

	
	@Override
	public Integer getRefStart() {
		return minRefStart;
	}

	@Override
	public Integer getRefEnd() {
		return maxRefEnd;
	}

}
