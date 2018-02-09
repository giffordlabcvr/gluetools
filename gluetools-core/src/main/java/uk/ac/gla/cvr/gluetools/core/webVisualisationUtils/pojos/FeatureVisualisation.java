package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils.pojos;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class FeatureVisualisation {

	@PojoDocumentField
	public String referenceName;

	@PojoDocumentField
	public String referenceDisplayName;

	@PojoDocumentField
	public String featureName;
	
	@PojoDocumentField
	public String featureDisplayName;

	@PojoDocumentField
	public Integer uNtWidth;
	
	@PojoDocumentListField(itemClass = VisualisationAnnotationRow.class)
	public List<VisualisationAnnotationRow> annotationRows = new ArrayList<VisualisationAnnotationRow>();
}
