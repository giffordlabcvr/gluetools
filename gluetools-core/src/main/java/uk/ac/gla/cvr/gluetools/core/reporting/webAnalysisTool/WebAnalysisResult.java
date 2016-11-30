package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentClass;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentListField;

@PojoDocumentClass
public class WebAnalysisResult {

	
	@PojoDocumentListField(fieldName = "featureAnalysis", itemClass = FeatureAnalysis.class)
	public List<FeatureAnalysis> featureAnalysisList;

	@PojoDocumentListField(fieldName = "referenceAnalysis", itemClass = ReferenceAnalysis.class)
	public List<ReferenceAnalysis> referenceAnalysisList;

	@PojoDocumentListField(fieldName = "queryAnalysis", itemClass = QueryAnalysis.class)
	public List<QueryAnalysis> queryAnalysisList;

	@PojoDocumentListField(fieldName = "variationCategoryResult", itemClass = VariationCategoryResult.class)
	public List<VariationCategoryResult> variationCategoryResultList;
	
	
	public WebAnalysisResult(
			List<FeatureAnalysis> featureAnalysisList, 
			List<ReferenceAnalysis> referenceAnalysisList, 
			List<QueryAnalysis> queryAnalysisList,
			List<VariationCategoryResult> variationCategoryResultList) {
		this.featureAnalysisList = featureAnalysisList;
		this.referenceAnalysisList = referenceAnalysisList;
		this.queryAnalysisList = queryAnalysisList;
		this.variationCategoryResultList = variationCategoryResultList;
	}

}
