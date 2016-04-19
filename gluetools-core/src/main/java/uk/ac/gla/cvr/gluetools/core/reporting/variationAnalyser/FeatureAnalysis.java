package uk.ac.gla.cvr.gluetools.core.reporting.variationAnalyser;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class FeatureAnalysis {

	@PojoResultField
	public String featureName;
	
	@PojoResultField
	public List<FeatureCodonLabel> featureCodonLabel;
	
}
