package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class WebAnalysisResult {

	@PojoResultField(resultName = "featureAnalysis")
	public List<FeatureAnalysis> featureAnalysisList;

	@PojoResultField(resultName = "referenceAnalysis")
	public List<ReferenceAnalysis> referenceAnalysisList;

	@PojoResultField(resultName = "queryAnalysis")
	public List<QueryAnalysis> queryAnalysisList;

	@PojoResultField(resultName = "variationCategoryResult")
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
