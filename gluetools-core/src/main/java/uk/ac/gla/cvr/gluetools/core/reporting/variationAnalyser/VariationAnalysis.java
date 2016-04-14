package uk.ac.gla.cvr.gluetools.core.reporting.variationAnalyser;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class VariationAnalysis {

	@PojoResultField(resultName = "sequenceAnalysis")
	public List<SequenceAnalysis> sequenceAnalysisList;

	public VariationAnalysis(List<SequenceAnalysis> sequenceAnalysisList) {
		this.sequenceAnalysisList = sequenceAnalysisList;
	}


}
