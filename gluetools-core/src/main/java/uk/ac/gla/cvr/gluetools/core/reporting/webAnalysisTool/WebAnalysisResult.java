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

	@PojoResultField(resultName = "sequenceAnalysis")
	public List<SequenceAnalysis> sequenceAnalysisList;

	public WebAnalysisResult(
			List<FeatureAnalysis> featureAnalysisList, 
			List<ReferenceAnalysis> referenceAnalysisList, 
			List<SequenceAnalysis> sequenceAnalysisList) {
		this.featureAnalysisList = featureAnalysisList;
		this.referenceAnalysisList = referenceAnalysisList;
		this.sequenceAnalysisList = sequenceAnalysisList;
	}


}
