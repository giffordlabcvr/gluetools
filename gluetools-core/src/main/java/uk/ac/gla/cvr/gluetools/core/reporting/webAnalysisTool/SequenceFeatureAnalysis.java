package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class SequenceFeatureAnalysis<C extends Aa, D extends NtSegment> {

	@PojoDocumentField
	public String featureName;
	
	@PojoDocumentListField(itemClass = Aa.class)
	public List<C> aas;

	@PojoDocumentListField(itemClass = NtSegment.class)
	public List<D> nts;

}
