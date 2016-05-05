package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultClass;
import uk.ac.gla.cvr.gluetools.core.command.result.PojoResultField;

@PojoResultClass
public class FeatureAnalysis {

	@PojoResultField
	public String featureName;
	
	@PojoResultField
	public List<CodonLabel> codonLabel;
	
	@PojoResultField
	public Integer startUIndex;

	@PojoResultField
	public Integer endUIndex;

	@PojoResultField
	public Boolean includesSequenceContent;

	@PojoResultField
	public String deriveSequenceContentFrom;
	
}
