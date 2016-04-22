package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class SequenceFeatureAnalysis {

	@PojoResultField
	public String featureName;
	
	@PojoResultField
	public List<Aa> aas;

	
}
