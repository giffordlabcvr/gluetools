package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentField;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class FeatureAnalysis {

	@PojoDocumentField
	public String featureName;
	
	@PojoDocumentListField(itemClass = CodonLabel.class)
	public List<CodonLabel> codonLabel;
	
	@PojoDocumentField
	public Integer startUIndex;

	@PojoDocumentField
	public Integer endUIndex;

	@PojoDocumentField
	public Boolean includesSequenceContent;

	@PojoDocumentField
	public String deriveSequenceAnalysisFrom;
	
}
